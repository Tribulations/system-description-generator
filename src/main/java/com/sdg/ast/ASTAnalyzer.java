package com.sdg.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.sdg.logging.LoggerUtil;
import com.sdg.graph.GraphDatabaseOperations;

/**
 * Analyzes Java Abstract Syntax Trees (AST) and stores the analysis results in a graph database.
 * JavaParser is used to parse the source code into an AST.
 *
 * This class can currently traverse the AST to identify:
 * - Classes and their methods
 * - Method calls between different methods
 * - Inheritance
 * - Interface implementations
 * - Class fields
 * 
 * The analysis results are stored using {@link GraphDatabaseOperations}.
 *
 * The analyzer can be configured to analyze different parts of the AST by passing an {@link ASTAnalyzerConfig}.
 * By default, the analyzer will analyze all parts of the AST.
 *
 * Example configuration to turn off analysis of methods, method calls and class fields:
 * ```java
 * ASTAnalyzerConfig config = new ASTAnalyzerConfig();
 * config.analyzeMethods(false)
 * .analyzeMethodCalls(false)
 * .analyzeClassFields(false);
 * ```
 *
 * @see <a href="https://javadoc.io/doc/com.github.javaparser/javaparser-core/latest/index.html">JavaParser Documentation</a>
 * @author Joakim Colloz
 * @version 1.0
 */
public class ASTAnalyzer {
    private final GraphDatabaseOperations dbOps;
    private final ASTAnalyzerConfig config;

    public ASTAnalyzer(GraphDatabaseOperations dbOps) {
        this.dbOps = dbOps;
        this.config = new ASTAnalyzerConfig();
        LoggerUtil.info(getClass(), "Created ASTAnalyzer with default configuration");
    }

    /*
     * Constructor for configuring what to analyze by passing an ASTAnalyzerConfig.
     *
     */
    public ASTAnalyzer(GraphDatabaseOperations dbOps, ASTAnalyzerConfig config) {
        this.dbOps = dbOps;
        this.config = config;
        LoggerUtil.info(getClass(), "Created ASTAnalyzer with configuration: {}", config);
    }
    
    /**
     * Analyze the given AST and store the analysis results in the graph database.
     *
     * @param cu the AST to analyze
     */

    public void analyzeAndStore(CompilationUnit cu) {
        LoggerUtil.info(getClass(), "Starting AST analysis");
        analyzeClass(cu);
        LoggerUtil.info(getClass(), "AST analysis completed");
    }

    private void analyzeClass(CompilationUnit cu) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(classDecl -> {
            String className = classDecl.getNameAsString();

            LoggerUtil.debug(getClass(), "Analyzing class: {}", className);

            // Extract package name
            String packageName = cu.getPackageDeclaration()
                    .map(NodeWithName::getNameAsString)
                    .orElse("<None>");

            LoggerUtil.debug(getClass(), "Found package for class {}: {}", className, packageName);
            dbOps.createClassNode(className, packageName);

            analyzeInheritance(classDecl, className);
            analyzeInterfaceImplementations(classDecl, className);
            analyzeImports(cu, className);
            analyzeFields(classDecl, className);
            analyzeMethods(classDecl, className);
        });
    }

    private void analyzeImports(CompilationUnit cu, String className) {
        if (!config.isAnalyzeImports()) {
            LoggerUtil.debug(getClass(), "Skipping imports analysis due to configuration");
            return;
        }

        cu.getImports().forEach(importDecl -> {
            String importName = importDecl.getNameAsString();
            LoggerUtil.debug(getClass(), "Found import in {}: {}", className, importName);
            dbOps.createImportRelationship(className, importName);
        });
    }

    private void analyzeInheritance(ClassOrInterfaceDeclaration classDecl, String className) {
        if (!config.isAnalyzeInheritance()) {
            LoggerUtil.debug(getClass(), "Skipping inheritance analysis due to configuration");
            return;
        }

        classDecl.getExtendedTypes().forEach(extendedType -> {
            String parentName = extendedType.getNameAsString();
            LoggerUtil.debug(getClass(), "Found inheritance: {} extends {}", className, parentName);
            dbOps.createClassNode(parentName); // Creates a class node if it does not already exist
            dbOps.createInheritanceRelationship(className, parentName);
        });
    }

    private void analyzeInterfaceImplementations(ClassOrInterfaceDeclaration classDecl, String className) {
        if (!config.isAnalyzeInterfaceImplementations()) {
            LoggerUtil.debug(getClass(), "Skipping interface implementations analysis due to configuration");
            return;
        }

        classDecl.getImplementedTypes().forEach(implementedType -> {
            String interfaceName = implementedType.getNameAsString();
            LoggerUtil.debug(getClass(), "Found interface implementation: {} implements {}", className, interfaceName);
            dbOps.createInterfaceImplementation(className, interfaceName);
        });
    }

    private void analyzeMethods(ClassOrInterfaceDeclaration classDecl, String className) {
        if (!config.isAnalyzeMethods()) {
            LoggerUtil.debug(getClass(), "Skipping methods analysis due to configuration");
            return;
        }

        classDecl.getMethods().forEach(method -> {
            String methodName = method.getNameAsString();
            LoggerUtil.debug(getClass(), "Analyzing method: {}.{}", className, methodName);
            dbOps.createMethodNode(className, methodName);
            analyzeMethodCalls(method, methodName);
            analyzeControlFlow(method, methodName);
        });
    }

    private void analyzeFields(ClassOrInterfaceDeclaration classDecl, String className) {
        if (!config.isAnalyzeClassFields()) {
            LoggerUtil.debug(getClass(), "Skipping class fields analysis due to configuration");
            return;
        }

        classDecl.getFields().forEach(field -> {
            String fieldType = field.getElementType().asString();
            String accessModifier = field.getModifiers().isEmpty() ? "package-private" :
                              field.getModifiers().get(0).toString().toLowerCase();

            field.getVariables().forEach(var -> {
                String fieldName = var.getNameAsString();
                LoggerUtil.debug(getClass(), "Found field: {}.{} ({} {})", className, fieldName, accessModifier, fieldType);
                dbOps.createClassField(className, fieldName, fieldType, accessModifier);
            });
        });
    }

    private void analyzeMethodCalls(MethodDeclaration method, String methodName) {
        if (!config.isAnalyzeMethodCalls()) {
            LoggerUtil.debug(getClass(), "Skipping method calls analysis due to configuration");
            return;
        }

        method.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodCallName = methodCall.getNameAsString();
            LoggerUtil.debug(getClass(), "Found method call: {} -> {}", methodName, methodCallName);
            dbOps.createMethodCallNode(methodName, methodCallName);
        });
    }

    private void analyzeControlFlow(MethodDeclaration method, String methodName) {
        if (!config.isAnalyzeControlFlow()) {
            LoggerUtil.debug(getClass(), "Skipping control flow analysis due to configuration");
            return;
        }

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
