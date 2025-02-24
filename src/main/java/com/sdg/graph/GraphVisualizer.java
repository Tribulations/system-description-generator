package com.sdg.graph;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import org.slf4j.Logger;
import com.sdg.logging.LoggerUtil;

public class GraphVisualizer {
    private static final Logger logger = LoggerUtil.getLogger(GraphVisualizer.class);

    public void visualize(CompilationUnit cu) {
        logger.info("Starting graph visualization");
        
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();
            logger.debug("Visualizing class: {}", className);

            classDecl.getMethods().forEach(method -> {
                String methodName = method.getNameAsString();
                logger.debug("Visualizing method: {}.{}", className, methodName);

                method.findAll(MethodCallExpr.class).forEach(methodCall -> {
                    String methodCallName = methodCall.getNameAsString();
                    logger.debug("Method call: {} -> {}", methodName, methodCallName);
                });

                method.findAll(IfStmt.class).forEach(ifStmt -> {
                    String condition = ifStmt.getCondition().toString();
                    logger.debug("Control flow (if): {}", condition);
                });

                method.findAll(ForStmt.class).forEach(forStmt -> {
                    String condition = forStmt.getCompare().toString();
                    logger.debug("Control flow (for): {}", condition);
                });
            });
        });
        
        logger.info("Graph visualization completed");
    }
}
