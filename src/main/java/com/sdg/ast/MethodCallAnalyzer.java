package com.sdg.ast;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class for performing an initial analysis of Java Abstract Syntax Trees (AST) to count method calls using JavaParser.
 * The method analyze() takes a Java file as input and returns a map of method names and the number of times they are called.
 *
 * @author Joakim Colloz
 * @version 1.0
 */
public class MethodCallAnalyzer {
    private TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
    private CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    private JavaSymbolSolver symbolSolver;
    private boolean isTest = false; // TODO: temporary workaround to include all directories when testing

    public MethodCallAnalyzer(final String rootDir) {
        initTypeSolvers(rootDir);
    }

    public MethodCallAnalyzer(final String rootDir, boolean isTest) {
        this.isTest = isTest;
        initTypeSolvers(rootDir);
    }

    /**
     * Analyze a Java file and return a map of the method signatures and the number of times they are called.
     * @param file the Java file to analyze
     * @return a map of the method signatures and the number of times they are called
     */
    public Map<String, Integer> analyze(final Path file) {
        LoggerUtil.debug(getClass(), "Starting method call analysis for file {}", file.toString());

        Map<String, Integer> methodCallsMap = new HashMap<>();

        JavaParser javaParser = new JavaParser();
        javaParser.getParserConfiguration()
                .setSymbolResolver(symbolSolver)
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                // Below configuration is for optimization, though, not sure if it makes a difference
                .setStoreTokens(false)
                .setAttributeComments(false)
                .setIgnoreAnnotationsWhenAttributingComments(true);

        try {
            ParseResult<CompilationUnit> compilationUnit = javaParser.parse(new File(file.toString()));
            resolveMethodCalls(compilationUnit.getResult().get(), methodCallsMap);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        LoggerUtil.debug(getClass(), "method call analysis of file {} completed", file.toString());

        return methodCallsMap;
    }

    private void initTypeSolvers(String rootDir) {
        // Create solvers
        reflectionTypeSolver = new ReflectionTypeSolver();
        combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(reflectionTypeSolver);

        // Add all directories in rootDir as source roots
        List<String> allDirsInRoot = getAllDirectoriesInRoot(rootDir);
        addDirectoriesToTypeSolvers(allDirsInRoot, combinedTypeSolver);

        // Create and configure the symbol solver
        this.symbolSolver = new JavaSymbolSolver(combinedTypeSolver);

        LoggerUtil.debug(getClass(), "Created MethodCallAnalyzer with rootDir: {}", rootDir);
    }

    /**
     * Returns a list of all directory paths under the given root directory.
     */
    private List<String> getAllDirectoriesInRoot(String rootDir) {
        List<String> dirs = new ArrayList<>();
        File root = new File(rootDir);
        if (root.exists() && root.isDirectory()) {
            collectDirectories(root, dirs);
        }
        return dirs;
    }

    private boolean isExcludedDirectory(File dir) {
        String name = dir.getName();

        if (isTest) {
            return false;
        } else {
            return name.equals("target") ||
                    name.equals("build") ||
                    name.equals("out") ||
                    name.equals("bin") ||
                    name.equals("node_modules") ||
                    name.equals(".idea") ||
                    name.equals(".vscode") ||
                    name.equals(".settings") ||
                    name.equals("dist") ||
                    name.equals("lib") ||
                    name.equals("logs") ||
                    name.equals("tmp") ||
                    name.equals("test-output") ||
                    name.equals("test") ||
                    name.equals("resources") ||
                    name.equals("scripts") ||
                    name.equals("scala") ||
                    name.equals("python") ||
                    name.equals("javascript") ||
                    name.equals("rust") ||
                    name.startsWith("."); // hidden dirs
        }
    }

    private void collectDirectories(File dir, List<String> dirs) {
        // Exclude certain directories
        if (isExcludedDirectory(dir)) {
            return;
        }

        if (dir.isDirectory()) {
            dirs.add(dir.getAbsolutePath());
            File[] children = dir.listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) {
                    collectDirectories(child, dirs);
                }
            }
        }
    }

    private void addDirectoriesToTypeSolvers(List<String> files, CombinedTypeSolver combinedTypeSolver) {
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

    private void resolveMethodCalls(CompilationUnit compilationUnit, Map<String, Integer> methodCallsMap) {
        compilationUnit.findAll(MethodCallExpr.class)
                .forEach(methodCall -> {
                    long start = System.currentTimeMillis(); // TODO for debugging

                    try {
                        String resolvedSignature = methodCall.resolve().getQualifiedSignature();

                        // For now, ignore calls to java.* methods (standard library)
                        if (resolvedSignature.startsWith("java")) {
                            return;
                        }

                        // Increment method call count
                        methodCallsMap.merge(resolvedSignature, 1, Integer::sum);
                        LoggerUtil.debug(getClass(), "Resolved method call: {} in {} ms", methodCall,
                                System.currentTimeMillis() - start);
                        
                    } catch (UnsolvedSymbolException e) {
                        String scope = methodCall.getScope().map(Object::toString).orElse("this");
                        LoggerUtil.debug(getClass(), "Failed to resolve: " + scope + "." +
                                methodCall.getNameAsString() + "() - " + e.getMessage());
                        LoggerUtil.debug(getClass(), "Failed to resolve:: {} in {} ms", methodCall,
                                System.currentTimeMillis() - start);

                    } catch (Exception e) {
                        LoggerUtil.debug(getClass(), "Failed to resolve:: {} in {} ms", methodCall,
                                System.currentTimeMillis() - start);
                    }
                });
    }
}
