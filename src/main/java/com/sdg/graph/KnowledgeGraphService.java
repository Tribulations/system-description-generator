package com.sdg.graph;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;

import org.neo4j.driver.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import static org.neo4j.driver.Values.parameters;

/**
 * This class uses JavaParser and Neo4j to create a very basic knowledge graph of a Java class.
 * Execute {@link KnowledgeGraphService#main} to test the class's usage.
 */
public class KnowledgeGraphService implements AutoCloseable {
    private final Driver driver;

    public KnowledgeGraphService() {
        this.driver = GraphDatabase.driver(
            Neo4jConfig.DB_URI, AuthTokens.basic(Neo4jConfig.DB_USER, Neo4jConfig.DB_PASSWORD)
        );
    }

    // Method to print the knowledge graph to the console
    public void printKnowledgeGraph(String filePath) {
        CompilationUnit cu = parseJavaFile(filePath);

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            System.out.println("Class: " + className);

            classDecl.getMethods().forEach(method -> {
                String methodName = method.getNameAsString();
                System.out.println("  Method: " + methodName);

                method.findAll(MethodCallExpr.class).forEach(methodCall -> {
                    String methodCallName = methodCall.getNameAsString();
                    System.out.println("    Method Call: " + methodCallName);
                });

                method.findAll(IfStmt.class).forEach(ifStmt -> {
                    String condition = ifStmt.getCondition().toString();
                    System.out.println("    ControlFlow (if): " + condition);
                });

                method.findAll(ForStmt.class).forEach(forStmt -> {
                    String condition = forStmt.getCompare().toString();
                    System.out.println("    ControlFlow (for): " + condition);
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
        CompilationUnit cu = parseJavaFile(filePath);

        try (Session session = driver.session()) {
            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
                String className = classDecl.getNameAsString();
                createClassNode(session, className);

                classDecl.getMethods().forEach(method -> {
                    String methodName = method.getNameAsString();
                    createMethodNodeAndConnectToClass(session, className, methodName);
                    processMethodCalls(session, method, methodName);
                    processControlFlow(session, method, methodName);
                });
            });
        }
    }

    private CompilationUnit parseJavaFile(String filePath) {
        try {
            return StaticJavaParser.parse(new FileInputStream(filePath));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void createClassNode(Session session, String className) {
        session.writeTransaction(tx -> {
            tx.run("MERGE (c:Class {name: $name})", parameters("name", className));
            return null;
        });
    }

    private void createMethodNodeAndConnectToClass(Session session, String className, String methodName) {
        session.writeTransaction(tx -> {
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
            session.writeTransaction(tx -> {
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
            session.writeTransaction(tx -> {
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
            session.writeTransaction(tx -> {
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
