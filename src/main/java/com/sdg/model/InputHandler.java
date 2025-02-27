package com.sdg.inputHandler;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class InputHandler {
    // TODO we have logger util
    private static final Logger logger = LoggerFactory.getLogger(InputHandler.class);

    /**
     * Processes input from a file, directory, or Git repository.
     * @param inputPath The path to a Java file, directory, or Git repository URL.
     * @return A list of Java source file paths.
     */
    public List<Path> processInput(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            logger.error("Provided input path is null or empty. Aborting processing.");
            return new ArrayList<>();
        }

        List<Path> javaFiles = new ArrayList<>();

        // Handle Git repository separately
        if (inputPath.startsWith("https://") || inputPath.startsWith("git@")) {
            logger.info("Detected Git repository. Cloning from: {}", inputPath);
            Path repoPath = cloneGitRepository(inputPath);
            if (repoPath != null) {
                javaFiles.addAll(collectJavaFiles(repoPath));
            }
            return javaFiles; // Return early to avoid further file path processing
        }

        Path path = Paths.get(inputPath); // This would cause an error if inputPath is a Git URL

        if (Files.isDirectory(path)) {
            logger.info("Processing directory: {}", inputPath);
            javaFiles.addAll(collectJavaFiles(path));
        } else if (Files.isRegularFile(path) && inputPath.endsWith(".java")) {
            logger.info("Processing single Java file: {}", inputPath);
            javaFiles.add(path);
        } else {
            logger.error("Invalid input path: {}. Expected a Java file, directory, or Git repository.", inputPath);
        }

        return javaFiles;
    }

    /**
     * Collects all Java files recursively from a given directory.
     * @param dirPath The directory to search for Java files.
     * @return A list of Java file paths.
     */
    private List<Path> collectJavaFiles(Path dirPath) {
        List<Path> javaFiles = new ArrayList<>();
        try {
            Files.walkFileTree(dirPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".java")) {
                        logger.debug("Identified Java source file: {}", file);
                        javaFiles.add(file);
                    } else {
                        logger.warn("Ignoring non-Java file: {}", file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.error("Failed to read directory: {}. Ensure it exists and is accessible.", dirPath, e);
        }
        return javaFiles;
    }

    /**
     * Clones a Git repository and returns the local path.
     * @param repoUrl The URL of the Git repository.
     * @return The local directory path of the cloned repository.
     */
    private Path cloneGitRepository(String repoUrl) {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("sdg-repo");
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .call();
            logger.info("Repository cloned successfully: {}", tempDir);
            return tempDir;
        } catch (IOException | GitAPIException e) {
            logger.error("Failed to clone repository: {}", repoUrl, e);
        }
        return null;
    }

    /**
     * Reads and preprocesses Java source files.
     * @param filePath The path to the Java file.
     * @return The normalized source code as a string.
     */
    public String readAndPreprocessFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            logger.error("File does not exist: {}", filePath);
            return "";
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                logger.warn("File is empty: {}", filePath);
                return "";
            }
            return content.replaceAll("\\s+", " ").trim(); // Normalize whitespace
        } catch (IOException e) {
            logger.error("Failed to read file: {}", filePath, e);
            return "";
        }
    }

    public static void main(String[] args) {
        InputHandler inputHandler = new InputHandler();

        // Test with a Git repository
        String repoUrl = "https://github.com/kishanrajput23/Java-Projects-Collections.git"; //repository URL
        List<Path> javaFiles = inputHandler.processInput(repoUrl);
        System.out.println("Java files from Git repo: " + javaFiles);

        // Read and preprocess a Java file
        if (!javaFiles.isEmpty()) {
            String fileContent = inputHandler.readAndPreprocessFile(javaFiles.getFirst());
            System.out.println("length of file content: \n" + fileContent.length());
            System.out.println("Processed content: \n" + fileContent);
        }
    }
}