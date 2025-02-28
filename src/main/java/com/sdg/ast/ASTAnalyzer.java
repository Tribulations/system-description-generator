package com.sdg.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.sdg.logging.LoggerUtil;
import com.sdg.graph.GraphDatabaseOperations;

/**
 * Analyzes Java Abstract Syntax Trees (AST) and stores the analysis results in a graph database.
 * This class can currently traverse the AST to identify:
 * - Classes and their methods
 * - Method calls between different methods
 * 
 * The analysis results are stored using {@link GraphDatabaseOperations}.
 * 
 * @author Joakim Colloz
 * @version 1.0
 */
public class ASTAnalyzer {
    private final GraphDatabaseOperations dbOps;

    public ASTAnalyzer(GraphDatabaseOperations dbOps) {
        this.dbOps = dbOps;
    }
    
    /**
     * Analyze the given AST and store the analysis results in the graph database.
     * @param cu the AST to analyze
     */

    public void analyzeAndStore(CompilationUnit cu) {
        LoggerUtil.info(getClass(), "Starting AST analysis");

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            LoggerUtil.debug(getClass(), "Analyzing class: {}", className);
            dbOps.createClassNode(className);

            classDecl.getMethods().forEach(method -> {
                String methodName = method.getNameAsString();
                LoggerUtil.debug(getClass(), "Analyzing method: {}.{}", className, methodName);

                dbOps.createMethodNode(className, methodName);
                analyzeMethodCalls(method, methodName);
                analyzeControlFlow(method, methodName);
            });
        });

        LoggerUtil.info(getClass(), "AST analysis completed");
    }

    private void analyzeMethodCalls(MethodDeclaration method, String methodName) {
        method.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodCallName = methodCall.getNameAsString();
            LoggerUtil.debug(getClass(), "Found method call: {} -> {}", methodName, methodCallName);
            dbOps.createMethodCallNode(methodName, methodCallName);
        });
    }

    private void analyzeControlFlow(MethodDeclaration method, String methodName) {
        method.findAll(IfStmt.class).forEach(ifStmt -> {
            String condition = ifStmt.getCondition().toString();
            LoggerUtil.debug(getClass(), "Found if statement in {}: {}", methodName, condition);
            dbOps.createControlFlowNode(methodName, "if", condition);
        });

        method.findAll(ForStmt.class).forEach(forStmt -> {
            String condition = forStmt.toString();
            LoggerUtil.debug(getClass(), "Found for loop in {}: {}", methodName, condition);
            dbOps.createControlFlowNode(methodName, "for", condition);
        });
    }
}
