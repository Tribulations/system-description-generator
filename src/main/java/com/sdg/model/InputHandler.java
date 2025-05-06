package com.sdg.model;

import com.sdg.logging.LoggerUtil;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private String systemName = "Not specified";

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
                            LoggerUtil.debug(getClass(), "Processing file: {}", file);
                            return new ProcessingResult(file, content.length(), content);
                        })
                        .toObservable()
                )
                .doOnComplete(() -> LoggerUtil.info(getClass(), "Completed processing all files."));
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
            LoggerUtil.error(getClass(), "Provided input path is null or empty. Aborting processing.");
            return new ArrayList<>();
        }

        List<Path> javaFiles = new ArrayList<>();
        // Check if input is a Git repository URL
        if (inputPath.startsWith("https://") || inputPath.startsWith("git@")) {
            LoggerUtil.info(getClass(), "Detected Git repository. Cloning from: {}", inputPath);
            Path repoPath = cloneGitRepository(inputPath);
            if (repoPath != null) {
                javaFiles.addAll(collectJavaFiles(repoPath));
            }
            return javaFiles;
        }

        Path path = Paths.get(inputPath);
        if (Files.isDirectory(path)) {
            LoggerUtil.debug(getClass(), "Processing directory: {}", inputPath);
            javaFiles.addAll(collectJavaFiles(path));
        } else if (Files.isRegularFile(path) && inputPath.endsWith(".java")) {
            LoggerUtil.debug(getClass(), "Processing single Java file: {}", inputPath);
            javaFiles.add(path);
        } else {
            LoggerUtil.error(getClass(), "Invalid input path: {}. Expected a Java file, directory, or Git repository.", inputPath);
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
        try (Stream<Path> pathStream = Files.walk(rootDir)) {
            pathStream
                    .filter(Files::isRegularFile) // Select only files, not directories
                    .filter(this::isRelevantJavaFile)
                    .forEach(file -> {
                        LoggerUtil.debug(getClass(), "Identified Java file: {}", file);
                        javaFiles.add(file);
                    });
        } catch (IOException e) {
            LoggerUtil.error(getClass(), "Failed to scan directory {}: {}", rootDir, e);
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
            Path tempDir = Files.createTempDirectory("git-clone-");
            Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(tempDir.toFile())
                    .call();
            LoggerUtil.info(getClass(), "Successfully cloned repository to: {}", tempDir);
            return tempDir;
        } catch (IOException | GitAPIException e) {
            LoggerUtil.error(getClass(), "Failed to clone repository: {}", repoUrl, e);
            return null;
        }
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
            LoggerUtil.warn(getClass(), "Failed to read file: {} | Error: {}", filePath, throwable.getMessage());
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
            LoggerUtil.error(getClass(), "File does not exist: {}", filePath);
            return "";
        }
        try {
            return Files.readString(filePath, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        } catch (MalformedInputException e) {
            LoggerUtil.warn(getClass(), "Malformed input detected in file: {}. Trying ISO-8859-1 as fallback.", filePath);
            try {
                return Files.readString(filePath,
                        StandardCharsets.ISO_8859_1).replaceAll("\\s+", " ").trim();
            } catch (IOException ex) {
                LoggerUtil.error(getClass(), "Failed to read file with fallback encoding: {}", filePath, ex);
            }
        } catch (IOException e) {
            LoggerUtil.error(getClass(), "Failed to read file: {}", filePath, e);
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
    }

    /**
     * Extracts a system name from the input path.
     * For Git repositories, uses the repository name.
     * For directories, uses the directory name.
     *
     * @param inputPath the path to extract system name from
     */
    public String extractSystemName(String inputPath) throws IllegalArgumentException {
        if (inputPath == null || inputPath.isBlank()) {
            throw new IllegalArgumentException("Input path is null or blank");
        }

        // For Git repositories
        if (inputPath.startsWith("https://") || inputPath.startsWith("git@")) {
            // Extract repository name from URL
            String repoName;
            if (inputPath.endsWith(".git")) {
                repoName = inputPath.substring(inputPath.lastIndexOf('/') + 1, inputPath.length() - 4);
            } else {
                repoName = inputPath.substring(inputPath.lastIndexOf('/') + 1);
            }
            systemName = repoName;
            LoggerUtil.info(getClass(), "Using system name from Git repository: {}", systemName);
        } else {
            // For file system paths
            Path path = Path.of(inputPath);
            if (Files.isDirectory(path)) {
                systemName = path.getFileName().toString();
                LoggerUtil.info(getClass(), "Using system name from directory: {}", systemName);
            } else if (Files.isRegularFile(path)) {
              reduce-json-size
                // TODO for single file throw exception? Only allow selecting whole project folder
                // For single file, use parent directory name
                systemName = path.getParent().getFileName().toString();
                LoggerUtil.info(getClass(), "Using system name from parent directory: {}", systemName);
            }
        }

        return systemName;
    }
}
