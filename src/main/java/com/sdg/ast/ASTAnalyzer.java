package com.sdg.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import org.slf4j.Logger;
import com.sdg.logging.LoggerUtil;
import com.sdg.graph.GraphDatabaseOperations;

public class ASTAnalyzer {
    private static final Logger logger = LoggerUtil.getLogger(ASTAnalyzer.class);
    private final GraphDatabaseOperations dbOps;

    public ASTAnalyzer(GraphDatabaseOperations dbOps) {
        this.dbOps = dbOps;
    }

    public void analyzeAndStore(CompilationUnit cu) {
        logger.info("Starting AST analysis");

        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            logger.debug("Analyzing class: {}", className);
            dbOps.createClassNode(className);

            classDecl.getMethods().forEach(method -> {
                String methodName = method.getNameAsString();
                logger.debug("Analyzing method: {}.{}", className, methodName);

                dbOps.createMethodNode(className, methodName);
                analyzeMethodCalls(method, methodName);
                analyzeControlFlow(method, methodName);
            });
        });

        logger.info("AST analysis completed");
    }

    private void analyzeMethodCalls(MethodDeclaration method, String methodName) {
        method.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodCallName = methodCall.getNameAsString();
            logger.debug("Found method call: {} -> {}", methodName, methodCallName);
            dbOps.createMethodCallNode(methodName, methodCallName);
        });
    }

    private void analyzeControlFlow(MethodDeclaration method, String methodName) {
        method.findAll(IfStmt.class).forEach(ifStmt -> {
            String condition = ifStmt.getCondition().toString();
            logger.debug("Found if statement in {}: {}", methodName, condition);
            dbOps.createControlFlowNode(methodName, "if", condition);
        });

        method.findAll(ForStmt.class).forEach(forStmt -> {
            String condition = forStmt.toString();
            logger.debug("Found for loop in {}: {}", methodName, condition);
            dbOps.createControlFlowNode(methodName, "for", condition);
        });
    }
}
