package com.sdg.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.sdg.logging.LoggerUtil;
import com.sdg.graph.GraphDatabaseOperations;

import java.util.Map;

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

    public void analyzeAndStore(CompilationUnit cu, Map<String, Integer> methodCallsMap) {
        LoggerUtil.debug(getClass(), "Starting AST analysis");
        analyzeClass(cu, methodCallsMap);
        LoggerUtil.debug(getClass(), "AST analysis completed");
    }

    private void analyzeClass(CompilationUnit cu, Map<String, Integer> methodCallsMap) {
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
            analyzeMethods(classDecl, className, methodCallsMap);
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

    private void analyzeMethods(ClassOrInterfaceDeclaration classDecl, String className, Map<String, Integer> methodCallsMap) {
        if (!config.isAnalyzeMethods()) {
            LoggerUtil.debug(getClass(), "Skipping methods analysis due to configuration");
            return;
        }

        LoggerUtil.debug(getClass(), "Analyzing methods for class: {} (only analyze public methods: {})",
                className, config.isOnlyAnalyzePublicMethods());

        classDecl.getMethods().forEach(method -> {
            boolean isPublic = method.isPublic();
            String methodName = method.getNameAsString();

            String methodFullyQualifiedName = classDecl.getFullyQualifiedName().get() + "." + method.getSignature();

            if (!methodCallsMap.containsKey(methodFullyQualifiedName)) {
                LoggerUtil.debug(getClass(), "Skipping method due to no calls: {}", methodFullyQualifiedName);
                return;
            } else {
                LoggerUtil.debug(getClass(), "Including method: {}", methodFullyQualifiedName);
            }

            // Skip non-public methods if specified by configuration
            if (!config.isOnlyAnalyzePublicMethods() || isPublic) {
                String visibility = isPublic ? "public" :
                                   method.isPrivate() ? "private" :
                                   method.isProtected() ? "protected" : "package-private";

                String returnType = method.getType().asString();
                NodeList<Parameter> methodParameters =method.getParameters();
                StringBuilder paramBuilder = new StringBuilder();
                if (!methodParameters.isEmpty()) {
                    methodParameters.forEach(param -> paramBuilder.append(param.getType().asString()).append(", "));
                }


                LoggerUtil.debug(getClass(), "Analyzing method: {}.{} with visibility {}",
                        className, methodName, visibility);

                dbOps.createMethodNode(className, methodName, visibility, returnType, paramBuilder.toString());

                analyzeMethodCalls(method, methodName);
                analyzeControlFlow(method, methodName);
            } else {
                LoggerUtil.debug(getClass(), "Skipping non-public method due to onlyAnalyzePublicMethods=true: {}.{}",
                        className, methodName);
            }
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

        LoggerUtil.debug(getClass(), "Analyzing method calls for method: {} (omit private method calls: {})",
                methodName, config.isOmitPrivateMethodCalls());

        // Get the parent class declaration to check for private methods if needed
        ClassOrInterfaceDeclaration parentClass = method.findAncestor(ClassOrInterfaceDeclaration.class)
                .orElse(null);
        
        method.findAll(MethodCallExpr.class).forEach(methodCall -> {
            String methodCallName = methodCall.getNameAsString();
            boolean shouldAnalyze = true;
            
            // Omit private method calls within the same class if specified by configuration
            if (config.isOmitPrivateMethodCalls() && parentClass != null) {
                // Check if the called method exists in the same class and is private
                boolean isPrivateMethodInSameClass = parentClass.getMethods().stream()
                    .anyMatch(m -> m.getNameAsString().equals(methodCallName) && m.isPrivate());
                
                if (isPrivateMethodInSameClass) {
                    LoggerUtil.debug(getClass(), "Skipping private method call within same class: {} -> {}", 
                            methodName, methodCallName);
                    shouldAnalyze = false;
                }
            }
            
            if (shouldAnalyze) {
                LoggerUtil.debug(getClass(), "Found method call: {} -> {}", methodName, methodCallName);
                dbOps.createMethodCallNode(methodName, methodCallName);
            }
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
