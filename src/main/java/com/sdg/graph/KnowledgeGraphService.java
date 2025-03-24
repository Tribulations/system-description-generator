package com.sdg.graph;

import com.github.javaparser.ast.CompilationUnit;
import com.sdg.ast.ASTAnalyzer;
import com.sdg.ast.ASTAnalyzerConfig;
import com.sdg.ast.JavaFileParser;
import com.sdg.client.LLMService;
import com.sdg.logging.LoggerUtil;
import com.sdg.model.InputHandler;
import com.sdg.model.InputHandler.ProcessingResult;
import io.reactivex.rxjava3.core.Observable;
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
 * // TODO: Add LLMService
 * // TODO: Add InputHandler
 * // TODO: Extract batch/session management to separate class?
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

    // TODO add java doc
    public KnowledgeGraphService() {
        LoggerUtil.info(getClass(), "Initializing KnowledgeGraphService");

        this.dbOps = new GraphDatabaseOperations();
        initializeSchema();

        this.parser = new JavaFileParser();
        this.analyzer = new ASTAnalyzer(dbOps);
        this.visualizer = new GraphVisualizer();
        this.inputHandler = new InputHandler();  // Initialize InputHandler
        this.llmService = new LLMService();
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
        this.llmService = new LLMService();
    }

    public Observable<ProcessingResult> processKnowledgeGraph(String inputPath) {
        LoggerUtil.info(getClass(), "Processing knowledge graph for path: {}", inputPath);

        long start = System.currentTimeMillis();

        // Start batch session before processing any files
        if (!dbOps.isBatchSessionActive()) {
            dbOps.startBatchSession();
        }

        return inputHandler.processFilesRx(inputPath)
                .observeOn(Schedulers.io())  // Keep processing on I/O thread
                .doOnNext(this::processFile)
                .doOnError(this::handleError)
                .doOnComplete(() -> {
                    LoggerUtil.info(getClass(), "All files processed successfully.");

                    // Commit any remaining transactions
                    if (dbOps.isBatchTransactionActive()) {
                        dbOps.commitBatchTransaction();
                    }

                    LoggerUtil.info(getClass(), "Processing all files took {} seconds.",
                            (System.currentTimeMillis() - start) / 1000);

                    // TODO: Extract this logic to a separate method as we do not want to execute this logic every test
                    // TODO : add button in UI to generate LLM response after all files have been processed
                    // Generate LLM response and handle it when it's ready
//                    generateLLMResponseAsync()
//                        .thenAccept(this::printLLMResponseToConsole)
//                        .exceptionally(this::handleLLMResponseError);
                });
    }

    /**
     * Use GraphDataToJsonConverter to get the knowledge graph as a JSON string
     *
     * @return the knowledge graph as a JSON string
     */
    private String getKnowledgeGraphAsJson() {
        String json;
        try {
            json = GraphDataToJsonConverter.getTopLevelNodesAsJSONString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return json;
    }

    /**
     * Generates the LLM response asynchronously based on the current knowledge graph.
     * This method is called to get the LLM response after all files have been processed.
     *
     * @return a CompletableFuture containing the LLM response
     */
    private CompletableFuture<String> generateLLMResponseAsync() {
        LoggerUtil.info(getClass(),
                "Generating LLM response asynchronously after all files have been processed");
        String json = getKnowledgeGraphAsJson();

        return llmService.generateHighLevelDescriptionAsync(json);
    }

    /**
     * Prints the LLM response to the console with formatting.
     *
     * @param response the LLM response to print
     */
    private void printLLMResponseToConsole(String response) {
        LoggerUtil.info(getClass(), "Received LLM response, printing to console");
        System.out.println("\n=== System Description Generated by LLM ===");
        System.out.println(response);
        System.out.println("===========================================\n");
    }

    /**
     * Handles errors that occur during LLM response generation.
     *
     * @param ex the exception that occurred
     * @return null to satisfy the CompletableFuture.
     */
    private Void handleLLMResponseError(Throwable ex) {
        LoggerUtil.error(getClass(), "Error getting LLM response: {}", ex.getMessage(), ex);
        System.err.println("Failed to generate system description: " + ex.getMessage());
        return null;
    }

    private void processFile(ProcessingResult result) {
        LoggerUtil.info(getClass(), "Processing file: {}", result.file());

        // Start a new transaction for this file if one isn't already active
        ensureActiveBatchTransaction();
        
        try {
            // Process the file and update batch commit status
            processFileAndManageBatch(result);
        } catch (Exception e) {
            handleFileProcessingError(e);
        }
    }

    /**
     * Ensures that a batch transaction is active before processing a file.
     * If no transaction is active a new one is started.
     */
    private void ensureActiveBatchTransaction() {
        if (!dbOps.isBatchTransactionActive()) {
            dbOps.startBatchTransaction();
        }
    }

    /**
     * Processes a file by inserting it into the graph database and manages batch commits.
     * This is achieved by calling the methods {@link #insertToGraphDatabase(Path)} and {@link #manageBatchCommits}.
     * 
     * @param result the processing result containing the file to process
     */
    private void processFileAndManageBatch(ProcessingResult result) {
        insertToGraphDatabase(result.file());
        manageBatchCommits();
    }
    

    /**
     * Checks if the current count has reached the batch commit threshold and commits the transaction if needed.
     * This method is synchronized to ensure thread safety when multiple files are processed in parallel.
     */
    private synchronized void manageBatchCommits() {
        // Increment the processed files count
        int currentCount = processedFilesCount.incrementAndGet();
        LoggerUtil.debug(getClass(), "Processed {} files", currentCount);

        // Commit the transaction if we've reached the threshold
        if (currentCount % BATCH_COMMIT_THRESHOLD == 0) {
            LoggerUtil.info(getClass(), "Reached batch commit threshold ({}). Committing transaction.",
                    BATCH_COMMIT_THRESHOLD);
            
            // Only commit if there's an active transaction
            if (dbOps.isBatchTransactionActive()) {
                dbOps.commitBatchTransaction();
                // Start a new transaction for the next batch
                dbOps.startBatchTransaction();
            }
        }
    }

    private void insertToGraphDatabase(Path filePath) {
        LoggerUtil.info(getClass(), "Inserting knowledge graph to database for file: {}", filePath);
        // Use JavaFileParser to parse the file and ASTAnalyzer for analysis
        CompilationUnit cu = parser.parseFile(filePath.toString());
        analyzer.analyzeAndStore(cu);
    }

    private void printKnowledgeGraph(Path filePath) {
        LoggerUtil.info(getClass(), "Printing knowledge graph for file: {}", filePath);
        // Use JavaFileParser to parse the file and GraphVisualizer for visualization
        CompilationUnit cu = parser.parseFile(filePath.toString());
        visualizer.visualize(cu);
    }

    private void handleError(Throwable throwable) {
        LoggerUtil.error(getClass(), "Error processing file: {}", throwable.getMessage(), throwable);
        // Ensure batch session is ended even if an error occurs
        dbOps.endBatchSession();
    }

    /**
     * Handles errors that occur during file processing.
     * Rolls back the current transaction and starts a new one.
     * 
     * @param e the exception that occurred during processing
     */
    private void handleFileProcessingError(Exception e) {
        LoggerUtil.error(getClass(), "Error processing file: {}", e.getMessage(), e);
        // Ensure transaction is rolled back if an error occurs
        if (dbOps.isBatchTransactionActive()) {
            try {
                // Roll back the transaction to avoid partial commits
                dbOps.rollbackBatchTransaction();
            } catch (Exception rollbackEx) {
                LoggerUtil.error(getClass(), "Error rolling back transaction: {}",
                        rollbackEx.getMessage(), rollbackEx);
            } finally {
                // Start a new transaction for the next file
                dbOps.startBatchTransaction();
            }
        }
    }

    public void deleteAllData() {
        LoggerUtil.info(getClass(), "Deleting all data from the graph database");

        // End any active batch transaction
        if (dbOps.isBatchTransactionActive()) {
            dbOps.commitBatchTransaction();
        }

        // End any active batch session
        if (dbOps.isBatchSessionActive()) {
            dbOps.endBatchSession();
        }

        // Reset the processed files counter
        processedFilesCount.set(0);

        // Delete all data
        dbOps.deleteAllData();
    }

    @Override
    public void close() {
        LoggerUtil.info(getClass(), "Closing KnowledgeGraphService");
        // Ensure any active transaction is committed before closing
        if (dbOps.isBatchTransactionActive()) {
            dbOps.commitBatchTransaction();
        }

        // End any active batch session
        if (dbOps.isBatchSessionActive()) {
            dbOps.endBatchSession();
        }

        dbOps.close();
    }

    /**
     *  Initialize the schema if it doesn't exist.
     */
    private void initializeSchema() {
        SchemaInitializer initializer = new SchemaInitializer(dbOps.getDriver());
        initializer.initializeSchema();
    }
}
