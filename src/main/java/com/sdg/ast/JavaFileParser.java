package com.sdg.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.sdg.logging.LoggerUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class JavaFileParser {
    public CompilationUnit parseFile(String filePath) {
        LoggerUtil.debug(getClass(), "Parsing Java file: {}", filePath);
        try {
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(filePath));
            LoggerUtil.info(getClass(), "Successfully parsed Java file: {}", filePath);
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
