package com.sdg.ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.slf4j.Logger;
import com.sdg.logging.LoggerUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class JavaFileParser {
    private static final Logger logger = LoggerUtil.getLogger(JavaFileParser.class);

    public CompilationUnit parseFile(String filePath) {
        logger.debug("Parsing Java file: {}", filePath);
        try {
            CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(filePath));
            logger.info("Successfully parsed Java file: {}", filePath);
            return cu;
        } catch (FileNotFoundException e) {
            logger.error("Failed to parse Java file: {}. File not found.", filePath, e);
            throw new RuntimeException("Failed to parse Java file: " + filePath, e);
        } catch (Exception e) {
            logger.error("Error parsing Java file: {}", filePath, e);
            throw new RuntimeException("Error parsing Java file: " + filePath, e);
        }
    }
}
