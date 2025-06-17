package com.sdg.graph;

import com.github.javaparser.ast.CompilationUnit;
import com.sdg.ast.ASTAnalyzer;
import com.sdg.ast.ASTAnalyzerConfig;
import com.sdg.ast.JavaFileParser;
import com.sdg.ast.MethodAnalysisHelper;
import com.sdg.ast.MethodCallAnalyzer;
import com.sdg.llm.GeminiApiClient;
import com.sdg.llm.LLMService;
import com.sdg.logging.LoggerUtil;
import com.sdg.model.InputHandler;
import com.sdg.model.InputHandler.ProcessingResult;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class orchestrates the creation of a knowledge graph from Java source code and generates
 * a high-level description using {@link LLMService}. //TODO: extract usage of LLMService to separate class?
 * It delegates the different responsibilities to specialized classes:
 * - {@link JavaFileParser}
 * - {@link ASTAnalyzer}
 * - {@link GraphDatabaseOperations}
 * - {@link GraphVisualizer}
 *
 * @author Joakim Colloz
 * @version 1.0
 */
public class KnowledgeGraphService implements AutoCloseable {
    private final JavaFileParser parser;
    private final GraphDatabaseOperations dbOps;
    private final ASTAnalyzer analyzer;
    private final GraphVisualizer visualizer;
    private final InputHandler inputHandler;
    private final LLMService llmService;
    private final AtomicInteger processedFilesCount = new AtomicInteger(0);
    private static final int BATCH_COMMIT_THRESHOLD = 10; // Commit after every 10 files
    private String systemName;
    private final MethodAnalysisHelper methodAnalysisHelper;

    /**
     * Default constructor initializes the service components.
     */
    public KnowledgeGraphService() {
        this(new ASTAnalyzerConfig());
    }

    /**
     * Constructor accepting a {@link ASTAnalyzerConfig}.
     *
     * @param config the configuration for the ASTAnalyzer
     */
    public KnowledgeGraphService(final ASTAnalyzerConfig config) {
        LoggerUtil.info(getClass(), "Initializing KnowledgeGraphService");

        this.dbOps = new GraphDatabaseOperations();
        initializeSchema();

        this.parser = new JavaFileParser();
        this.analyzer = new ASTAnalyzer(dbOps, config);
        this.visualizer = new GraphVisualizer();
        this.inputHandler = new InputHandler();  // Initialize InputHandler
        this.llmService = new LLMService(new GeminiApiClient());
        this.methodAnalysisHelper = new MethodAnalysisHelper();
    }
    
    /**
     * Constructor accepting a {@link ASTAnalyzerConfig} and a method filter percentage.
     *
     * @param config the configuration for the ASTAnalyzer
     * @param methodFilterPercentage percentage of methods to filter out (0.0-1.0)
     */
    public KnowledgeGraphService(final ASTAnalyzerConfig config, double methodFilterPercentage) {
        this(config);
        this.methodAnalysisHelper.setMethodFilterPercentage(methodFilterPercentage);
    }
    
    /**
     * Sets the percentage of methods to filter out from each class.
     * The filtering keeps methods with the most calls and removes the rest.
     *
     * @param percentage value between 0.0 (no filtering) and 1.0 (filter all methods)
     * @throws IllegalArgumentException if percentage is not between 0.0 and 1.0
     */
    public void setMethodFilterPercentage(double percentage) {
        methodAnalysisHelper.setMethodFilterPercentage(percentage);
    }
    
    /**
     * Gets the current method filter percentage.
     *
     * @return the current method filter percentage (0.0-1.0)
     */
    public double getMethodFilterPercentage() {
        return methodAnalysisHelper.getMethodFilterPercentage();
    }

    /**
     * Processes Java source files to build the knowledge graph.
     *
     * @param inputPath Path to the source files.
     * @return Observable stream of processing results.
     */
    public Observable<ProcessingResult> processKnowledgeGraph(String inputPath, boolean resetDatabase) {
        if (resetDatabase) {
            dbOps.deleteAllData();
        }

        LoggerUtil.info(getClass(), "Processing knowledge graph for path: {}", inputPath);

        // Extract system name from input path
        this.systemName = inputHandler.extractSystemName(inputPath);

        long start = System.currentTimeMillis();
        ensureBatchSession();
        MethodCallAnalyzer methodCallAnalyzer = new MethodCallAnalyzer(inputPath);

        return inputHandler.processFilesRx(inputPath)
                .subscribeOn(Schedulers.io())
                .map(ProcessingResult::file)
                .observeOn(Schedulers.computation())
                .flatMap(file -> Observable.fromCallable(() -> MethodAnalysisHelper.countMethodCalls(file, methodCallAnalyzer)))
                .toList()
                .map(methodAnalysisHelper::processMethodAnalysisResult)
                .flatMapObservable(methodAnalysisResult -> {
                    Map<String, Integer> methodCallsMap = methodAnalysisResult.methodCallsMap();
                    // Chain file processing as a Completable, then emit ProcessingResult after all files processed
                    return Observable.fromIterable(methodAnalysisResult.files())
                            .observeOn(Schedulers.io())
                            .flatMapCompletable(file -> Completable.fromAction(() -> processFile(file, methodCallsMap)))
                            .doOnComplete(() -> finalizeProcessing(start))
                            .doOnError(this::handleError)
                            .andThen(Observable.just(new ProcessingResult(Path.of(inputPath), 0, "")));
                });
    }

    /**
     * Ensures a batch session is active before processing files.
     */
    private void ensureBatchSession() {
        if (!dbOps.isBatchSessionActive()) {
            dbOps.startBatchSession();
        }
    }

    /**
     * Finalizes processing by committing transactions and logging the execution time.
     */
    private void finalizeProcessing(long start) {
        LoggerUtil.info(getClass(), "All files analyzed successfully.");
        commitRemainingTransactions();
        LoggerUtil.info(getClass(), "Processing took {} seconds.", (System.currentTimeMillis() - start) / 1000);
    }

    private void commitRemainingTransactions() {
        if (dbOps.isBatchTransactionActive()) {
            dbOps.commitBatchTransaction();
        }
    }

    private void processFile(Path file, Map<String, Integer> methodCallsMap) {
        ProcessingResult result = new ProcessingResult(file, 0, "");
        LoggerUtil.debug(getClass(), "Processing file: {}", result.file());
        ensureActiveBatchTransaction();

        try {
            insertToGraphDatabase(file, methodCallsMap);
            manageBatchCommits();
        } catch (Exception e) {
            handleFileProcessingError(e);
        }
    }

    private void ensureActiveBatchTransaction() {
        if (!dbOps.isBatchTransactionActive()) {
            dbOps.startBatchTransaction();
        }
    }

    private void insertToGraphDatabase(Path filePath, Map<String, Integer> methodCallsMap) {
        CompilationUnit cu = parser.parseFile(filePath.toString());
        analyzer.analyzeAndStore(cu, methodCallsMap);
    }

    private synchronized void manageBatchCommits() {
        if (processedFilesCount.incrementAndGet() % BATCH_COMMIT_THRESHOLD == 0) {
            commitRemainingTransactions();
            dbOps.startBatchTransaction();
        }
    }

    private void handleError(Throwable throwable) {
        LoggerUtil.error(getClass(), "Error processing file: {}", throwable.getMessage(), throwable);
        dbOps.endBatchSession();
    }

    private void handleFileProcessingError(Exception e) {
        LoggerUtil.error(getClass(), "Error processing file: {}", e.getMessage(), e);
        rollbackTransactionSafely();
    }

    private void rollbackTransactionSafely() {
        if (dbOps.isBatchTransactionActive()) {
            try {
                dbOps.rollbackBatchTransaction();
            } catch (Exception rollbackEx) {
                LoggerUtil.error(getClass(), "Error rolling back transaction: {}", rollbackEx.getMessage(), rollbackEx);
            } finally {
                dbOps.startBatchTransaction();
            }
        }
    }

    public Single<String> generateLLMResponseAsync() {
        return Single.create(emitter -> {
            // Get the CompletableFuture from the LLMService
            CompletableFuture<String> future = llmService.generateHighLevelDescriptionAsync(getKnowledgeGraphAsJson());

            // Handle success
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    emitter.onError(ex);  // Propagate error to RxJava
                } else {
                    emitter.onSuccess(result);  // Emit result to RxJava
                }
            });
        });
    }

    private String getKnowledgeGraphAsJson() {
        try {
            return GraphDataToJsonConverter.buildTopLevelNodesAsJSONString(systemName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        commitRemainingTransactions();
        dbOps.endBatchSession();
        dbOps.close();
    }

    private void initializeSchema() {
        new SchemaInitializer(dbOps.getDriver()).initializeSchema();
    }
}
