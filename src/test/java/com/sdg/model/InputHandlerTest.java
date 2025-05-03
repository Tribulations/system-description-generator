package com.sdg.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link InputHandler} class.
 */
public class InputHandlerTest {

    private InputHandler inputHandler;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        inputHandler = new InputHandler();
    }
    
    /**
     * Tests extracting system name from an HTTPS Git repository URL.
     * Example: "https://github.com/username/repo-name"
     */
    @Test
    void testExtractSystemNameFromHttpsGitRepository() {
        String gitUrl = "https://github.com/username/repo-name";
        String systemName = inputHandler.extractSystemName(gitUrl);
        
        assertEquals("repo-name", systemName);
    }
    
    /**
     * Tests extracting system name from an HTTPS Git repository URL with .git extension.
     * Example: "https://github.com/username/repo-name.git"
     */
    @Test
    void testExtractSystemNameFromHttpsGitRepositoryWithDotGit() {
        String gitUrl = "https://github.com/username/repo-name.git";
        String systemName = inputHandler.extractSystemName(gitUrl);
        
        assertEquals("repo-name", systemName);
    }
    
    /**
     * Tests extracting system name from an SSH Git repository URL.
     * Example: "git@github.com:username/repo-name"
     */
    @Test
    void testExtractSystemNameFromSshGitRepository() {
        String gitUrl = "git@github.com:username/repo-name";
        String systemName = inputHandler.extractSystemName(gitUrl);
        
        assertEquals("repo-name", systemName);
    }
    
    /**
     * Tests extracting system name from an SSH Git repository URL with .git extension.
     * Example: "git@github.com:username/repo-name.git"
     */
    @Test
    void testExtractSystemNameFromSshGitRepositoryWithDotGit() {
        String gitUrl = "git@github.com:username/repo-name.git";
        String systemName = inputHandler.extractSystemName(gitUrl);
        
        assertEquals("repo-name", systemName);
    }

    /**
     * Tests extracting system name from a directory path.
     * Example: "/tmp/test-project"
     */
    @Test
    void testExtractSystemNameFromDirectory() throws IOException {
        // Create a test directory
        Path testDir = tempDir.resolve("test-project");
        Files.createDirectory(testDir);

        String systemName = inputHandler.extractSystemName(testDir.toString());

        assertEquals("test-project", systemName);
    }

    /**
     * Tests extracting system name from a nested directory path.
     * Example: "/tmp/junit12345/test-project"
     */
    @Test
    void testExtractSystemNameFromNestedDirectory() throws IOException {
        // Create a parent directory
        Path rootDir = tempDir.resolve("junit12345");
        Files.createDirectory(rootDir);

        // Create a test directory inside the parent directory
        Path testDir = rootDir.resolve("test-project");
        Files.createDirectory(testDir);

        String systemName = inputHandler.extractSystemName(testDir.toString());

        assertEquals("test-project", systemName);
    }
    
    /**
     * Tests extracting system name from a file path.
     * For a file, it should use the parent directory name
     * Example: "/tmp/junit12345/parent-project/TestFile.java"
     */
    @Test
    void testExtractSystemNameFromFile() throws IOException {
        // Create a test directory and file
        Path testDir = tempDir.resolve("parent-project");
        Files.createDirectory(testDir);
        Path testFile = testDir.resolve("TestFile.java");
        Files.createFile(testFile);
        
        String systemName = inputHandler.extractSystemName(testFile.toString());
        
        assertEquals("parent-project", systemName);
    }
    
    /**
     * Tests that an IllegalArgumentException is thrown when null input is provided.
     */
    @Test
    void testExtractSystemNameWithNullInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                inputHandler.extractSystemName(null));
        
        String expectedMessage = "Input path is null or blank";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
    
    /**
     * Tests that an IllegalArgumentException is thrown when empty input is provided.
     */
    @Test
    void testExtractSystemNameWithEmptyInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                inputHandler.extractSystemName(""));
        
        String expectedMessage = "Input path is null or blank";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
    
    /**
     * Tests that an IllegalArgumentException is thrown when blank input (only whitespace) is provided.
     */
    @Test
    void testExtractSystemNameWithBlankInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                inputHandler.extractSystemName("   "));
        
        String expectedMessage = "Input path is null or blank";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
