package com.sdg.model;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Handles file input processing, including reading Java files from directories, individual files,
 * and cloning Git repositories to extract Java source files.
 *
 * @author Suraj Karki
 */
public class InputHandler {
    // TODO obs we have logger util
    private static final Logger logger = LoggerFactory.getLogger(InputHandler.class);

    /**
     * Processes files reactively by reading and preprocessing each Java file found in the input path.
     *
     * @param inputPath The path to a file, directory, or Git repository.
     * @return An Observable emitting ProcessingResult for each Java file.
     */
    public Observable<ProcessingResult> processFilesRx(String inputPath) {
        return processInputRx(inputPath)
                .flattenAsObservable(paths -> paths)  // Emits each file separately
                .flatMap(file -> readAndPreprocessFileRx(file)
                        .subscribeOn(Schedulers.io())  // Process each file on a separate I/O thread
                        .map(content -> {
                            logger.info("Processing file: " + file);
                            return new ProcessingResult(file, content.length(), content);
                        })
                        .toObservable()
                )
                .doOnComplete(() -> logger.info("Completed processing all files."));
    }

    /**
     * Processes the given input path reactively and returns a list of Java files found.
     *
     * @param inputPath The file path or repository URL.
     * @return A Single emitting a list of Java file paths.
     */
    private Single<List<Path>> processInputRx(String inputPath) {
        return Single.fromCallable(() -> processInput(inputPath)).subscribeOn(Schedulers.io());
    }

    /**
     * Determines whether the input is a directory, a single Java file, or a Git repository,
     * then collects the relevant Java source files.
     *
     * @param inputPath The path to a file, directory, or Git repository.
     * @return A list of Java file paths.
     */
    private List<Path> processInput(String inputPath) {
        if (inputPath == null || inputPath.isBlank()) {
            logger.error("Provided input path is null or empty. Aborting processing.");
            return new ArrayList<>();
        }

        List<Path> javaFiles = new ArrayList<>();

        // Check if input is a Git repository URL
        if (inputPath.startsWith("https://") || inputPath.startsWith("git@")) {
            logger.info("Detected Git repository. Cloning from: {}", inputPath);
            Path repoPath = cloneGitRepository(inputPath);
            if (repoPath != null) {
                javaFiles.addAll(collectJavaFiles(repoPath));
            }
            return javaFiles;
        }

        Path path = Paths.get(inputPath);
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
     * Recursively collects Java files from the given directory.
     *
     * @param rootDir The root directory to scan.
     * @return A list of Java file paths.
     */
    private List<Path> collectJavaFiles(Path rootDir) {
        List<Path> javaFiles = new ArrayList<>();

        if (rootDir == null || !Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            logger.error("Invalid directory: {}", rootDir);
            return javaFiles;
        }

        try (Stream<Path> paths = Files.walk(rootDir).parallel()) {
            paths.filter(Files::isRegularFile)  // Select only files, not directories
                    .filter(this::isRelevantJavaFile) 
                    .forEach(file -> {
                        logger.debug("Identified Java file: {}", file);
                        javaFiles.add(file);
                    });
        } catch (IOException e) {
            logger.error("Failed to scan directory {}: {}", rootDir, e.getMessage());
        }

        return javaFiles;
    }

    /**
     * Clones a Git repository into a temporary directory.
     *
     * @param repoUrl The URL of the Git repository.
     * @return The local path to the cloned repository or null if cloning fails.
     */
    private Path cloneGitRepository(String repoUrl) {
        try {
            Path tempDir = Files.createTempDirectory("sdg-repo");
            Git.cloneRepository().setURI(repoUrl).setDirectory(tempDir.toFile()).call();
            logger.info("Repository cloned successfully: {}", tempDir);
            return tempDir;
        } catch (IOException | GitAPIException e) {
            logger.error("Failed to clone repository: {}", repoUrl, e);
        }
        return null;
    }

    /**
     * Reads and preprocesses the content of a file reactively.
     * If an error occurs, an empty string is returned.
     *
     * @param filePath The path to the Java file.
     * @return A Single emitting the file's processed content.
     */
    private Single<String> readAndPreprocessFileRx(Path filePath) {
        return Single.fromCallable(() ->
                readAndPreprocessFile(filePath)).onErrorReturn(throwable -> {
            logger.warn("Failed to read file: {} | Error: {}", filePath, throwable.getMessage());
            return "";
        });
    }

    /**
     * Reads a file's content, removes excessive whitespace, and trims the result.
     * If an error occurs, an empty string is returned.
     *
     * @param filePath The path to the Java file.
     * @return The preprocessed file content.
     */
    private String readAndPreprocessFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            logger.error("File does not exist: {}", filePath);
            return "";
        }
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        } catch (MalformedInputException e) {
            logger.warn("Malformed input detected in file: {}. Trying " +
                    "ISO-8859-1 as fallback.\n", filePath);
            try {
                return Files.readString(filePath,
                        StandardCharsets.ISO_8859_1).replaceAll("\\s+", " ").trim();
            } catch (IOException ex) {
                logger.error("Failed to read file with fallback encoding: {}", filePath, ex);
            }
        } catch (IOException e) {
            logger.error("Failed to read file: {}", filePath, e);
        }
        return "";
    }

    /**
     * Represents the result of processing a Java file. Only for debug purpose!
     */
    public record ProcessingResult(Path file, int contentLength,
                                   String processedContent) {
    }

    private boolean isRelevantJavaFile(Path file) {
        String filePath = file.toString().toLowerCase();
        return filePath.endsWith(".java") &&
                !filePath.contains("test") &&
                !filePath.contains("config") &&
                !filePath.contains("util") &&
                !filePath.contains("utils");
        // TODO: we also might want to omit exception classes
    }

//    public static void main(String[] args) {
//        InputHandler inputHandler = new InputHandler();
//        String repoUrl = "https://github.com/kishanrajput23/Java-Projects-Collections.git";
//
//        inputHandler.processFilesRx(repoUrl)
//                .blockingForEach(result -> {
//                    System.out.println("File: " + result.file());
//                    System.out.println("Length of file content: " + result.contentLength());
//                    System.out.println("Processed content: \n" + result.processedContent());
//                });
//    }
}