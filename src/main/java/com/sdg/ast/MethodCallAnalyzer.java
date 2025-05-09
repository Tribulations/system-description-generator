package com.sdg.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.sdg.logging.LoggerUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for performing an initial analysis of Java Abstract Syntax Trees (AST) to count method calls using JavaParser.
 */
public class MethodCallAnalyzer {
//    private final Map<String, List<String>> methodDefinitions = new HashMap<>();
    private final Map<String, Integer> methodCallsMap = new HashMap<>();

    public MethodCallAnalyzer() {
        LoggerUtil.debug(getClass(), "Created MethodCallAnalyzer with default configuration");
    }

    public Map<String, Integer> getMethodCallsMap() {
        return methodCallsMap;
    }

    public void analyze(List<String> files, String rootDir) {
        LoggerUtil.debug(getClass(), "Starting method call analysis");
        JavaFileParser parser = new JavaFileParser();

        // Create solvers
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);

        // Add all directories in rootDir as source roots
        List<String> allDirsInRoot = getAllDirectoriesInRoot(rootDir);
        addDirectoriesForAnalyzedFiles(allDirsInRoot, combinedTypeSolver);

        // Also add parent directories of analyzed files
        addDirectoriesForAnalyzedFiles(files, combinedTypeSolver);

        // Create and configure the symbol solver
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);

        // Put called methods in map
        for (String file : files) {
            try {
                CompilationUnit compilationUnit = StaticJavaParser.parse(new File(file));
                resolveMethodCalls(compilationUnit);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        LoggerUtil.debug(getClass(), "method call analysis completed");
    }

    /**
     * Returns a list of all directory paths (recursively) under the given root directory.
     */
    private List<String> getAllDirectoriesInRoot(String rootDir) {
        List<String> dirs = new ArrayList<>();
        File root = new File(rootDir);
        if (root.exists() && root.isDirectory()) {
            collectDirectoriesRecursive(root, dirs);
        }
        return dirs;
    }

    private void collectDirectoriesRecursive(File dir, List<String> dirs) {
        if (dir.isDirectory()) {
            dirs.add(dir.getAbsolutePath());
            File[] children = dir.listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) {
                    collectDirectoriesRecursive(child, dirs);
                }
            }
        }
    }

    private void addDirectoriesForAnalyzedFiles(List<String> files, CombinedTypeSolver combinedTypeSolver) {
        Set<String> directories = new HashSet<>();
        for (String filePath : files) {
            File file = new File(filePath);
            String directoryPath = file.getParent();
            if (directoryPath != null && !directories.contains(directoryPath)) {
                try {
                    combinedTypeSolver.add(new JavaParserTypeSolver(new File(directoryPath)));
                    directories.add(directoryPath);
                    LoggerUtil.debug(getClass(), "Added directory to type solver: " + directoryPath);
                } catch (Exception e) {
                    LoggerUtil.error(getClass(), "Could not add directory to type solver: " + directoryPath);
                }
            }
        }
    }

    private void resolveMethodCalls(CompilationUnit compilationUnit) {
        compilationUnit.findAll(MethodCallExpr.class)
                .forEach(methodCall -> {
                    try {
                        String resolvedSignature = methodCall.resolve().getQualifiedSignature();

                        if (resolvedSignature.startsWith("java")) {
                            return;
                        }

                        methodCallsMap.compute(resolvedSignature, (k, methodCallCount)
                                -> methodCallCount == null ? 1
                                : ++methodCallCount);

                    } catch (UnsolvedSymbolException e) {
                        String scope = methodCall.getScope().map(Object::toString).orElse("this");
                        LoggerUtil.debug(getClass(), "Failed to resolve: " + scope + "." +
                                methodCall.getNameAsString() + "() - " + e.getMessage());
                    } catch (Exception e) {
                        LoggerUtil.error(getClass(), "Failed to resolve method call: {} - {}",
                                methodCall, e.getMessage());
                    }
                });
    }
}
