package com.sdg.ast;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.sdg.logging.LoggerUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * Parses Java source files into Abstract Syntax Trees (AST) using JavaParser.
 * This class provides a simple interface to parse Java files into {@link CompilationUnit}
 * objects that can be analyzed by {@link ASTAnalyzer}.
 * 
 * Handles file reading and parsing errors with appropriate logging and exception handling.
 * 
 * @author Joakim Colloz
 * @version 1.0
 */
public class JavaFileParser {
    /**
     * Parses a Java file into an Abstract Syntax Tree (AST).
     * 
     * @param filePath The path to the Java file to be parsed.
     * @return A CompilationUnit representing the parsed Java file.
     */
    public CompilationUnit parseFile(String filePath) {
        ParserConfiguration config = new ParserConfiguration();
        config.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        StaticJavaParser.setConfiguration(config);
        LoggerUtil.debug(getClass(), "Parsing Java file: {}", filePath);
        try {
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(filePath));
            LoggerUtil.debug(getClass(), "Successfully parsed Java file: {}", filePath);
            return cu;
        } catch (FileNotFoundException e) {
            LoggerUtil.error(getClass(), "Failed to parse Java file: {}. File not found.", filePath, e);
            throw new RuntimeException("Failed to parse Java file: " + filePath, e);
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "Error parsing Java file: {}", filePath, e);
            throw new RuntimeException("Error parsing Java file: " + filePath, e);
        }
    }
}
