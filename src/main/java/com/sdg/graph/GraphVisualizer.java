package com.sdg.graph;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.sdg.logging.LoggerUtil;

public class GraphVisualizer {
    public void visualize(CompilationUnit cu) {
        LoggerUtil.info(getClass(), "Starting graph visualization");
        
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            LoggerUtil.info(getClass(), "Visualizing class: {}", className);

            classDecl.getMethods().forEach(method -> {
                String methodName = method.getNameAsString();
                LoggerUtil.info(getClass(), "Visualizing method: {}.{}", className, methodName);

                method.findAll(MethodCallExpr.class).forEach(methodCall -> {
                    String methodCallName = methodCall.getNameAsString();
                    LoggerUtil.info(getClass(), "Method call: {} -> {}", methodName, methodCallName);
                });

                method.findAll(IfStmt.class).forEach(ifStmt -> {
                    String condition = ifStmt.getCondition().toString();
                    LoggerUtil.info(getClass(), "Control flow (if): {}", condition);
                });

                method.findAll(ForStmt.class).forEach(forStmt -> {
                    String condition = forStmt.getCompare().toString();
                    LoggerUtil.info(getClass(), "Control flow (for): {}", condition);
                });
            });
        });
        
        LoggerUtil.info(getClass(), "Graph visualization completed");
    }
}
