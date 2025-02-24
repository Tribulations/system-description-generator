package com.sdg.graph;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import org.neo4j.driver.*;
import org.slf4j.Logger;
import com.sdg.logging.LoggerUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.neo4j.driver.Values.parameters;

/**
 * This class uses JavaParser and Neo4j to create a very basic knowledge graph of a Java class.
 * Execute {@link KnowledgeGraphService#main} to test the class's usage.
 *
 * // TODO : Possible improvements:
 * Hierarchy modeling (e.g., :Class could be part of a broader :Entity category).
 * Method return types and parameters to enrich the graph.
 * Type inference (e.g., linking method calls to the actual methods they call).
 */
public class KnowledgeGraphService implements AutoCloseable {
    private static final Logger logger = LoggerUtil.getLogger(KnowledgeGraphService.class);
    private final Driver driver;

    public KnowledgeGraphService() {
        logger.info("Initializing KnowledgeGraphService");
        this.driver = GraphDatabase.driver(
            Neo4jConfig.DB_URI, AuthTokens.basic(Neo4jConfig.DB_USER, Neo4jConfig.DB_PASSWORD)
        );
        logger.debug("Neo4j driver initialized with URI: {}", Neo4jConfig.DB_URI);
    }

    // Method to print the knowledge graph to the console
    public void printKnowledgeGraph(String filePath) {
        logger.info("Printing knowledge graph for file: {}", filePath);
        CompilationUnit cu = parseJavaFile(filePath);

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            logger.debug("Found class: {}", className);

            classDecl.getMethods().forEach(method -> {
                String methodName = method.getNameAsString();
                logger.debug("Found method in class {}: {}", className, methodName);

                method.findAll(MethodCallExpr.class).forEach(methodCall -> {
                    String methodCallName = methodCall.getNameAsString();
                    logger.debug("Found method call in {}.{}: {}", className, methodName, methodCallName);
                });

                method.findAll(IfStmt.class).forEach(ifStmt -> {
                    String condition = ifStmt.getCondition().toString();
                    logger.debug("Found control flow (if) in {}.{}: {}", className, methodName, condition);
                });

                method.findAll(ForStmt.class).forEach(forStmt -> {
                    String condition = forStmt.getCompare().toString();
                    logger.debug("Found control flow (for) in {}.{}: {}", className, methodName, condition);
                });
            });
        });
    }

    /**
     * Uses JavaParser to parse the Java class and create a AST (Abstract Syntax Tree).
     * Then uses Neo4j to insert the AST into a graph database.
     * @param filePath
     */
    public void insertToGraphDatabase(String filePath) {
        logger.info("Inserting knowledge graph to database for file: {}", filePath);
        CompilationUnit cu = parseJavaFile(filePath);

        try (Session session = driver.session()) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                String className = classDecl.getNameAsString();
                logger.debug("Processing class for database insertion: {}", className);

                createClassNode(session, className);

                classDecl.getMethods().forEach(method -> {
                    String methodName = method.getNameAsString();
                    logger.debug("Processing method for database insertion: {}.{}", className, methodName);
                    createMethodNodeAndConnectToClass(session, className, methodName);
                    processMethodCalls(session, method, methodName);
                    processControlFlow(session, method, methodName);
                });
            });
            logger.info("Successfully inserted knowledge graph for file: {}", filePath);
        } catch (Exception e) {
            logger.error("Error inserting knowledge graph to database for file: {}", filePath, e);
            throw e;
        }
    }

    private CompilationUnit parseJavaFile(String filePath) {
        logger.debug("Parsing Java file: {}", filePath);
        try {
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(filePath));
            logger.info("Successfully parsed Java file: {}", filePath);
            return cu;
        } catch (FileNotFoundException e) {
            logger.error("Failed to parse Java file: {}. File not found.", filePath, e);
            throw new RuntimeException("Failed to parse Java file: " + filePath, e);
        } catch (Exception e) {
            logger.error("Error parsing Java file: {}", filePath, e);
            throw new RuntimeException("Error parsing Java file: " + filePath, e);
        }
    }

    private void createClassNode(Session session, String className) {
        session.writeTransaction(tx -> {
            logger.debug("Creating class node: {}", className);
            tx.run("MERGE (c:Class {name: $name})", parameters("name", className));
            return null;
        });
    }

    private void createMethodNodeAndConnectToClass(Session session, String className, String methodName) {
        session.writeTransaction(tx -> {
            logger.debug("Creating method node and relationship: {}.{}", className, methodName);
            tx.run("MERGE (m:Method {name: $name})", parameters("name", methodName));
            tx.run("MATCH (c:Class {name: $className}), (m:Method {name: $methodName}) " +
                            "MERGE (c)-[:HAS_METHOD]->(m)",
                    parameters("className", className, "methodName", methodName));
            return null;
        });
    }

    private void processMethodCalls(Session session, MethodDeclaration method, String methodName) {
        method.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodCallName = methodCall.getNameAsString();
            logger.debug("Processing method call: {}.{} -> {}", "", methodName, methodCallName);

            session.writeTransaction(tx -> {
                logger.debug("Creating method call node and relationship: {} -> {}", methodName, methodCallName);
                tx.run("MERGE (f:FunctionCall {name: $name})", parameters("name", methodCallName));
                tx.run("MATCH (m:Method {name: $methodName}), (f:FunctionCall {name: $name}) " +
                                "MERGE (m)-[:CALLS]->(f)",
                        parameters("methodName", methodName, "name", methodCallName));
                return null;
            });
        });
    }

    private void processControlFlow(Session session, MethodDeclaration method, String methodName) {
        method.findAll(IfStmt.class).forEach(ifStmt -> {
            String condition = ifStmt.getCondition().toString();
            logger.debug("Processing control flow (if): {}.{} -> {}", "", methodName, condition);

            session.writeTransaction(tx -> {
                logger.debug("Creating control flow node and relationship: {} -> {}", methodName, condition);
                tx.run("MERGE (ctrl:ControlFlow {type: 'if', condition: $condition})",
                        parameters("condition", condition));
                tx.run("MATCH (m:Method {name: $methodName}), (ctrl:ControlFlow {condition: $condition}) " +
                                "MERGE (m)-[:CONTAINS]->(ctrl)",
                        parameters("methodName", methodName, "condition", condition));
                return null;
            });
        });

        method.findAll(ForStmt.class).forEach(forStmt -> {
            String condition = forStmt.getCompare().toString();
            logger.debug("Processing control flow (for): {}.{} -> {}", "", methodName, condition);

            session.writeTransaction(tx -> {
                logger.debug("Creating control flow node and relationship: {} -> {}", methodName, condition);
                tx.run("MERGE (ctrl:ControlFlow {type: 'for', condition: $condition})",
                        parameters("condition", condition));
                tx.run("MATCH (m:Method {name: $methodName}), (ctrl:ControlFlow {condition: $condition}) " +
                                "MERGE (m)-[:CONTAINS]->(ctrl)",
                        parameters("methodName", methodName, "condition", condition));
                return null;
            });
        });
    }

    @Override
    public void close() {
        logger.info("Closing KnowledgeGraphService");
        if (driver != null) {
            driver.close();
        }
    }

    public static void main(String[] args) {
        try (Neo4jDatabaseService dbService = new Neo4jDatabaseService();
             KnowledgeGraphService graphService = new KnowledgeGraphService()) {

            // Clean the database first
            dbService.deleteAllData();

            // Print the knowledge graph to console
            // graphService.printKnowledgeGraph("src/main/java/com/sdg/TestClass.java");

            // Insert the knowledge graph into the database
            graphService.insertToGraphDatabase("src/main/java/com/sdg/graph/TestClass.java");
        }
    }
}
