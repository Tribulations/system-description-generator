package com.sdg.ast;

import com.sdg.logging.LoggerUtil;
import io.reactivex.rxjava3.annotations.NonNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for method analysis operations on results received from {@link MethodCallAnalyzer}.
 * This class handles the processing of method calls analysis results,
 * including merging method call maps and filtering methods based on call frequency.
 *
 * @author Joakim Colloz
 * @version 1.0
 */
public class MethodAnalysisHelper {
    private double methodFilterPercentage = 0.5;
    
    /**
     * Record representing the result using {@link MethodCallAnalyzer#analyze(Path)} 
     */
    public record MethodAnalysisResult(Path file, Map<String, Integer> methodCallsMap) {}
    
    /**
     * Record representing the processed result of a method analysis.
     */
    public record ProcessedMethodAnalysisResult(List<Path> files, Map<String, Integer> methodCallsMap) {}

    /**
     * Default constructor with default method filter percentage (0.5)
     */
    public MethodAnalysisHelper() {
    }

    /**
     * Constructor with specified method filter percentage
     * 
     * @param methodFilterPercentage percentage of methods to filter out (0.0-1.0)
     */
    public MethodAnalysisHelper(double methodFilterPercentage) {
        setMethodFilterPercentage(methodFilterPercentage);
    }

    /**
     * Sets the percentage of methods to filter out from each class.
     * The filtering keeps methods with the most calls and removes the rest.
     *
     * @param percentage value between 0.0 (no filtering) and 1.0 (filter all methods)
     * @throws IllegalArgumentException if percentage is not between 0.0 and 1.0
     */
    public void setMethodFilterPercentage(double percentage) {
        if (percentage < 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("Method filter percentage must be between 0.0 and 1.0");
        }
        this.methodFilterPercentage = percentage;
    }

    /**
     * Gets the current method filter percentage.
     *
     * @return the current method filter percentage (between 0.0-1.0)
     */
    public double getMethodFilterPercentage() {
        return methodFilterPercentage;
    }

    /**
     * Extracts the analyzed files and merges the method call counts received after analyzing method calls
     * using {@link MethodCallAnalyzer}.
     *
     * @param methodAnalysisResults the list of method analysis results to process
     * @return the processed method analysis result
     */
    public ProcessedMethodAnalysisResult processMethodAnalysisResult(@NonNull List<MethodAnalysisResult> methodAnalysisResults) {
        // Extract files and merge method call counts
        List<Path> files = methodAnalysisResults.stream().map(MethodAnalysisResult::file).toList();
        Map<String, Integer> methodCallsMap = mergeMethodCallCounts(methodAnalysisResults);
        
        // Apply method filtering if configured
        if (methodFilterPercentage > 0.0) {
            methodCallsMap = filterMethodsByCallFrequency(methodCallsMap);
        }

        return new ProcessedMethodAnalysisResult(files, methodCallsMap);
    }

    /**
     * Analyzes a Java source file to count method calls using the provided {@link MethodCallAnalyzer}.
     * 
     * @param file The path to the Java source file to analyze
     * @param analyzer The MethodCallAnalyzer instance to use for analysis
     * @return A MethodAnalysisResult containing the file path and a map of method calls with their frequencies
     */
    public static MethodAnalysisResult countMethodCalls(Path file, MethodCallAnalyzer analyzer) {
        return new MethodAnalysisResult(file, analyzer.analyze(file));
    }

    /**
     * Merges multiple method call maps into a single map, summing up the call counts.
     * 
     * @param results List of method analysis results to merge
     * @return A merged map of method calls with their combined frequencies
     */
    public Map<String, Integer> mergeMethodCallCounts(List<MethodAnalysisResult> results) {
        Map<String, Integer> mergedMap = new HashMap<>();
        for (MethodAnalysisResult result : results) {
            for (Map.Entry<String, Integer> entry : result.methodCallsMap().entrySet()) {
                mergedMap.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return mergedMap;
    }

    /**
     * Filters the methodCallsMap to keep only a specified percentage of methods per class.
     * Methods are kept based on the number of calls (most called methods are kept and at least one per class).
     *
     * @param methodCallsMap the original method calls map
     * @return filtered method calls map
     */
    public Map<String, Integer> filterMethodsByCallFrequency(Map<String, Integer> methodCallsMap) {
        Map<String, Map<String, Integer>> methodsByClass = groupMethodsByClass(methodCallsMap);

        // Filter methods for each class
        Map<String, Integer> filteredMethodCalls = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> classEntry : methodsByClass.entrySet()) {
            Map<String, Integer> classMethods = classEntry.getValue();
            int totalMethods = classMethods.size();
            int methodsToKeep = (int) Math.ceil(totalMethods * (1.0 - methodFilterPercentage));
            
            // Ensure we keep at least one method if there are any methods
            methodsToKeep = Math.max(methodsToKeep, totalMethods > 0 ? 1 : 0);
            
            LoggerUtil.debug(getClass(), "Class {}: Keeping {} out of {} methods", 
                    classEntry.getKey(), methodsToKeep, totalMethods);
            
            // Sort methods by call count (descending) and keep top methods
            List<Map.Entry<String, Integer>> sortedMethods = classMethods.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .toList();
            
            // Keep the top methods based on call count
            for (int i = 0; i < methodsToKeep && i < sortedMethods.size(); i++) {
                Map.Entry<String, Integer> entry = sortedMethods.get(i);
                filteredMethodCalls.put(entry.getKey(), entry.getValue());
            }
        }
        
        LoggerUtil.info(getClass(), "Filtered method calls map from {} to {} methods", 
                methodCallsMap.size(), filteredMethodCalls.size());
        
        return filteredMethodCalls;
    }

    private Map<String, Map<String, Integer>> groupMethodsByClass(Map<String, Integer> methodCallsMap) {
        Map<String, Map<String, Integer>> methodsByClass = new HashMap<>();

        for (Map.Entry<String, Integer> entry : methodCallsMap.entrySet()) {
            String methodSignature = entry.getKey();
            String className = extractClassName(methodSignature);

            if (!methodsByClass.containsKey(className)) {
                methodsByClass.put(className, new HashMap<>());
            }
            methodsByClass.get(className).put(methodSignature, entry.getValue());
        }
        return methodsByClass;
    }

    /**
     * Extracts the class name from a fully qualified method signature. Assumes the format: package.class.method(params)
     *
     * @param methodSignature the fully qualified method signature
     * @return the class name
     */
    public String extractClassName(String methodSignature) {
        int openParenIndex = methodSignature.indexOf('(');
        if (openParenIndex > 0) {
            String fullName = methodSignature.substring(0, openParenIndex);
            int lastDotIndex = fullName.lastIndexOf('.');
            
            if (lastDotIndex > 0) {
                String classWithPackage = fullName.substring(0, lastDotIndex);
                int lastDotInPackage = classWithPackage.lastIndexOf('.');
                if (lastDotInPackage > 0) {
                    return classWithPackage;
                }
                return classWithPackage;
            }
            return fullName; // No dots in the name
        }
        return methodSignature; // Fallback if format is unexpected
    }
}
