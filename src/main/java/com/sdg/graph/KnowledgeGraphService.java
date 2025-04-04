package com.sdg.graph;

import com.github.javaparser.ast.CompilationUnit;
import com.sdg.ast.ASTAnalyzer;
import com.sdg.ast.ASTAnalyzerConfig;
import com.sdg.ast.JavaFileParser;
import com.sdg.llm.GeminiApiClient;
import com.sdg.llm.LLMService;
import com.sdg.logging.LoggerUtil;
import com.sdg.model.InputHandler;
import com.sdg.model.InputHandler.ProcessingResult;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
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
    }

    /**
     * Processes Java source files to build the knowledge graph.
     *
     * @param inputPath Path to the source files.
     * @return Observable stream of processing results.
     */
    public Observable<ProcessingResult> processKnowledgeGraph(String inputPath) {
        LoggerUtil.info(getClass(), "Processing knowledge graph for path: {}", inputPath);

        long start = System.currentTimeMillis();
        ensureBatchSession();

        return inputHandler.processFilesRx(inputPath)
                .observeOn(Schedulers.io())
                .doOnNext(this::processFile)
                .doOnError(this::handleError)
                .doOnComplete(() -> finalizeProcessing(start));
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

    private void processFile(ProcessingResult result) {
        LoggerUtil.debug(getClass(), "Processing file: {}", result.file());
        ensureActiveBatchTransaction();

        try {
            insertToGraphDatabase(result.file());
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

    private void insertToGraphDatabase(Path filePath) {
        CompilationUnit cu = parser.parseFile(filePath.toString());
        analyzer.analyzeAndStore(cu);
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
            return GraphDataToJsonConverter.getTopLevelNodesAsJSONString();
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
