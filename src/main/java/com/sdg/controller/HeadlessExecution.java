package com.sdg.controller;

import com.sdg.graph.KnowledgeGraphService;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class HeadlessExecution {

    private static final Logger logger = LoggerFactory.getLogger(HeadlessExecution.class);

    private final KnowledgeGraphService graphService;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public HeadlessExecution(KnowledgeGraphService graphService) {
        this.graphService = graphService;
    }
    /**
     * Starts headless mode by requesting input from the terminal.
     */
    public void startHeadlessMode() {
        // Create a latch to wait for the completion of the entire process
        CountDownLatch latch = new CountDownLatch(1);

        disposables.add(
                // Request input for the file path or GitHub URL reactively
                promptUser("Enter local file path or GitHUb URl:")
                        .filter(input -> !input.isEmpty()) // Ensure input is not empty
                        .flatMapObservable(path -> {
                            String inputPath =convertHostPath(path);
                            // Process the graph in the background when input is provided
                            return graphService.processKnowledgeGraph(inputPath, true )
                                    .subscribeOn(Schedulers.io()) // Perform on background thread
                                    .doOnSubscribe(disposable -> System.out.println("Processing files..."))
                                    .doFinally(() -> System.out.println("Knowledge Graph created."));
                        })
                        .ignoreElements() // Wait for the graph processing to complete
                        .andThen(
                                // After processing, prompt user if they want to generate a description
                                promptUser("Do you want to generate the description now? (y/n):")
                                        .flatMap(answer -> {
                                            if (answer.equalsIgnoreCase("y")) {
                                                // If yes, generate the description asynchronously
                                                return graphService.generateLLMResponseAsync()
                                                        .observeOn(Schedulers.trampoline());  // Handle on the main thread
                                            } else {
                                                System.out.println("Aborted by user.");
                                                return Single.just("Operation aborted."); // Return a single with a message
                                            }
                                        })
                        )
                        .doFinally(latch::countDown) // Decrement latch when the operations are done
                        .subscribe(
                                response -> System.out.println("\nSystem Description: " + response), // Print response
                                this::handleLLMProcessingErrorHeadless // Handle error
                        )
        );

        // Block the main thread until the reactive pipeline completes
        try {
            latch.await();  // Wait until latch is decremented (operation completes)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted while waiting for completion.");
        }
    }

    /**
     * Prompt the user for input reactively.
     * @param message The prompt message.
     * @return A Single emitting the user's input.
     */
    private Single<String> promptUser(String message) {
        return Single.fromCallable(() -> {
            System.out.println(message);
            Scanner scanner = new Scanner(System.in);
            return scanner.nextLine().trim();  // Get user input asynchronously
        }).subscribeOn(Schedulers.io());  // Run on background thread
    }

    private String convertHostPath(String input) {
        return input.replaceFirst("^[A-Za-z]:\\\\", "/mnt/host/")
                .replace("\\", "/");
    }

    /**
     * Handles errors that occur during LLM response generation in headless mode.
     * Logs the error and displays a message in the terminal.
     * @param throwable The exception thrown during LLM response generation.
     */
    private void handleLLMProcessingErrorHeadless(Throwable throwable) {
        String errorMessage = "\nLLM Response Error: " + throwable.getMessage();
        System.err.println(errorMessage);
        logger.error(errorMessage, throwable);
    }
}
