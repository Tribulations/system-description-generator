package com.sdg.graph;

import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import com.sdg.logging.LoggerUtil;
import com.sdg.ast.JavaFileParser;
import com.sdg.ast.ASTAnalyzer;

/**
 * This class orchestrates the creation of a knowledge graph from Java source code.
 * It delegates the different responsibilities to specialized classes:
 * - {@link com.sdg.ast.JavaFileParser}
 * - {@link com.sdg.ast.ASTAnalyzer}
 * - {@link com.sdg.graph.GraphDatabaseOperations}
 * - {@link com.sdg.graph.GraphVisualizer}
 */
public class KnowledgeGraphService implements AutoCloseable {
    private static final Logger logger = LoggerUtil.getLogger(KnowledgeGraphService.class);

    private final JavaFileParser parser;
    private final GraphDatabaseOperations dbOps;
    private final ASTAnalyzer analyzer;
    private final GraphVisualizer visualizer;

    public KnowledgeGraphService() {
        logger.info("Initializing KnowledgeGraphService");
        this.parser = new JavaFileParser();
        this.dbOps = new GraphDatabaseOperations();
        this.analyzer = new ASTAnalyzer(dbOps);
        this.visualizer = new GraphVisualizer();
    }

    public void printKnowledgeGraph(String filePath) {
        logger.info("Printing knowledge graph for file: {}", filePath);
        CompilationUnit cu = parser.parseFile(filePath);
        visualizer.visualize(cu);
    }

    public void insertToGraphDatabase(String filePath) {
        logger.info("Inserting knowledge graph to database for file: {}", filePath);
        CompilationUnit cu = parser.parseFile(filePath);
        analyzer.analyzeAndStore(cu);
    }

    /**
     * Delegates to {@link com.sdg.graph.GraphDatabaseOperations#deleteAllData()}
     */
    public void deleteAllData() {
        dbOps.deleteAllData();
    }

    @Override
    public void close() {
        logger.info("Closing KnowledgeGraphService");
        dbOps.close();
    }

    public static void main(String... args) {
        try (KnowledgeGraphService graphService = new KnowledgeGraphService()) {

            // // Clean the database first
            graphService.deleteAllData();

            // // Insert the knowledge graph into the database
            graphService.insertToGraphDatabase("src/main/java/com/sdg/graph/TestClass.java");
        }
    }
}
