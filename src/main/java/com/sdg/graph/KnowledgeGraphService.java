package com.sdg.graph;

import com.github.javaparser.ast.CompilationUnit;
import com.sdg.ast.ASTAnalyzerConfig;
import com.sdg.logging.LoggerUtil;
import com.sdg.model.InputHandler;
import com.sdg.model.InputHandler.ProcessingResult;
import com.sdg.ast.JavaFileParser;
import com.sdg.ast.ASTAnalyzer;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import java.nio.file.Path;

/**
 * This class orchestrates the creation of a knowledge graph from Java source code.
 * It delegates the different responsibilities to specialized classes:
 * - {@link com.sdg.ast.JavaFileParser}
 * - {@link com.sdg.ast.ASTAnalyzer}
 * - {@link com.sdg.graph.GraphDatabaseOperations}
 * - {@link com.sdg.graph.GraphVisualizer}
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

    public KnowledgeGraphService() {
        LoggerUtil.info(getClass(), "Initializing KnowledgeGraphService");
        this.parser = new JavaFileParser();
        this.dbOps = new GraphDatabaseOperations();
        this.analyzer = new ASTAnalyzer(dbOps);
        this.visualizer = new GraphVisualizer();
        this.inputHandler = new InputHandler();  // Initialize InputHandler
    }

    /**
     * Constructor accepting a {@link com.sdg.ast.ASTAnalyzerConfig}.
     *
     * @param config the configuration for the ASTAnalyzer
     */
    public KnowledgeGraphService(final ASTAnalyzerConfig config) {
        LoggerUtil.info(getClass(), "Initializing KnowledgeGraphService");
        this.parser = new JavaFileParser();
        this.dbOps = new GraphDatabaseOperations();
        this.analyzer = new ASTAnalyzer(dbOps, config);
        this.visualizer = new GraphVisualizer();
        this.inputHandler = new InputHandler();  // Initialize InputHandler
    }

    public Observable<ProcessingResult> processKnowledgeGraph(String inputPath) {
        LoggerUtil.info(getClass(), "Processing knowledge graph for path: {}", inputPath);

        return inputHandler.processFilesRx(inputPath)
                .observeOn(Schedulers.io())  // Keep processing on I/O thread
                .doOnNext(this::processFile)
                .doOnError(this::handleError)
                .doOnComplete(() -> LoggerUtil.info(getClass(), "All files processed successfully."));
    }

    private void processFile(ProcessingResult result) {
        LoggerUtil.info(getClass(), "Processing file: {}", result.file());

        // Insert the knowledge graph into the database
        insertToGraphDatabase(result.file());

        // Print the knowledge graph
        printKnowledgeGraph(result.file());
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
    }

    public void deleteAllData() {
        dbOps.deleteAllData();
    }

    @Override
    public void close() {
        LoggerUtil.info(getClass(), "Closing KnowledgeGraphService");
        dbOps.close();
    }
}
