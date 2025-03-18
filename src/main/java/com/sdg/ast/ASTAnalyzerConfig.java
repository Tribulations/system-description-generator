package com.sdg.ast;

/**
 * Configuration class for the {@link ASTAnalyzer}.
 *
 * Configuration to turn on/off various features of the analyzer can be beneficial during development.
 *
 * @author Joakim Colloz
 */
public class ASTAnalyzerConfig {
    private boolean analyzeMethods = true;
    private boolean analyzeMethodCalls = true;
    private boolean analyzeClassFields = true;
    private boolean analyzeInheritance = true;
    private boolean analyzeImports = true;
    private boolean analyzeInterfaceImplementations = true;
    private boolean analyzeControlFlow = true;

    public ASTAnalyzerConfig analyzeMethods(boolean analyzeMethods) {
        this.analyzeMethods = analyzeMethods;
        return this;
    }

    public ASTAnalyzerConfig analyzeMethodCalls(boolean analyzeMethodCalls) {
        this.analyzeMethodCalls = analyzeMethodCalls;
        return this;
    }

    public ASTAnalyzerConfig analyzeClassFields(boolean analyzeClassFields) {
        this.analyzeClassFields = analyzeClassFields;
        return this;
    }

    public ASTAnalyzerConfig analyzeInheritance(boolean analyzeInheritance) {
        this.analyzeInheritance = analyzeInheritance;
        return this;
    }

    public ASTAnalyzerConfig analyzeImports(boolean analyzeImports) {
        this.analyzeImports = analyzeImports;
        return this;
    }

    public ASTAnalyzerConfig analyzeInterfaceImplementations(boolean analyzeInterfaceImplementations) {
        this.analyzeInterfaceImplementations = analyzeInterfaceImplementations;
        return this;
    }

    public ASTAnalyzerConfig analyzeControlFlow(boolean analyzeControlFlow) {
        this.analyzeControlFlow = analyzeControlFlow;
        return this;
    }

    // Setters

    public void setAnalyzeMethods(boolean analyzeMethods) {
        this.analyzeMethods = analyzeMethods;
    }

    public void setAnalyzeMethodCalls(boolean analyzeMethodCalls) {
        this.analyzeMethodCalls = analyzeMethodCalls;
    }

    public void setAnalyzeClassFields(boolean analyzeClassFields) {
        this.analyzeClassFields = analyzeClassFields;
    }

    public void setAnalyzeInheritance(boolean analyzeInheritance) {
        this.analyzeInheritance = analyzeInheritance;
    }

    public void setAnalyzeImports(boolean analyzeImports) {
        this.analyzeImports = analyzeImports;
    }

    public void setAnalyzeInterfaceImplementations(boolean analyzeInterfaceImplementations) {
        this.analyzeInterfaceImplementations = analyzeInterfaceImplementations;
    }

    public void setAnalyzeControlFlow(boolean analyzeControlFlow) {
        this.analyzeControlFlow = analyzeControlFlow;
    }

    // Getters

    public boolean isAnalyzeMethods() {
        return analyzeMethods;
    }

    public boolean isAnalyzeMethodCalls() {
        return analyzeMethodCalls;
    }

    public boolean isAnalyzeClassFields() {
        return analyzeClassFields;
    }

    public boolean isAnalyzeInheritance() {
        return analyzeInheritance;
    }

    public boolean isAnalyzeImports() {
        return analyzeImports;
    }

    public boolean isAnalyzeInterfaceImplementations() {
        return analyzeInterfaceImplementations;
    }

    public boolean isAnalyzeControlFlow() {
        return analyzeControlFlow;
    }


}
