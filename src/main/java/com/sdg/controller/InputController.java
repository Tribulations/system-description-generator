package com.sdg.controller;

import com.sdg.ast.ASTAnalyzerConfig;
import com.sdg.graph.KnowledgeGraphService;
import com.sdg.model.InputHandler.ProcessingResult;
import com.sdg.view.InputView;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.core.Scheduler;

import javax.swing.SwingUtilities;
import javax.swing.JFileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The InputController class handles user interactions in the UI, processes input files,
 * and manages asynchronous operations using RxJava.
 *
 * @author Suraj Karki
 */
public class InputController {
    private static final Logger logger = LoggerFactory.getLogger(InputController.class);

    private final InputView view;
    private final KnowledgeGraphService graphService;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Scheduler swingScheduler = Schedulers.from(SwingUtilities::invokeLater);

    /**
     * Constructor for InputController.
     * @param view  The UI component for user interaction.
     * @param graphService The service responsible for processing files and generating the knowledge graph.
     */
    public InputController(InputView view, KnowledgeGraphService graphService) {
        this.view = view;
        this.graphService = graphService;

        // Attach event listeners to the UI buttons
        view.addBrowseListener(e -> browseFile());
        view.addProcessListener(e -> processInput());
        view.addDecListener(e -> generateDescription());
    }

    /**
     * Opens a file chooser dialog for the user to select a file or directory.
     * Updates the UI with the selected path.
     */
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
            updateOutput("Selected: " + selectedPath);
            view.inputField.setText(selectedPath);
        }
    }

    /**
     * Processes the user-selected input path using RxJava.
     * Ensures UI updates are executed on the Swing Event Dispatch Thread (EDT).
     */
    private void processInput() {
        String inputPath = view.getInputPath().trim();

        // Validate user input before processing
        if (inputPath.isEmpty()) {
            updateOutput("Error: No input path provided.");
            return;
        }

        // Subscribe to the RxJava observable processing files asynchronously
        disposables.add(
                graphService.processKnowledgeGraph(inputPath)
                        .observeOn(swingScheduler)
                        .subscribe(
                                this::handleProcessingResult,
                                this::handleProcessingError
                        )
        );
    }

    /**
     * Handles successful file processing results.
     * Updates the UI with details of the processed file.
     * @param result The processing result containing file details and content.
     */
    private void handleProcessingResult(ProcessingResult result) {
        updateOutput("\nProcessing file: " + result.file());
    }

    /**
     * Handles errors that occur during file processing.
     * Logs the error and displays a message in the UI.
     * @param throwable The exception thrown during processing.
     */
    private void handleProcessingError(Throwable throwable) {
        String errorMessage = "\nError: " + throwable.getMessage();
        logger.error(errorMessage, throwable);
        updateOutput(errorMessage);
    }

    /**
     * Triggers LLM response generation and updates the UI with the generated response.
     */
    private void generateDescription() {
        disposables.add(
                graphService.generateLLMResponseAsync()
                        .observeOn(swingScheduler)
                        .subscribe(
                                response -> updateOutput("\nLLM Response: " + response),
                                this::handleLLMProcessingError
                        )
        );
    }
    /**
     * Handles errors that occur during LLM response generation.
     * Logs the error and displays a message in the UI.
     * @param throwable The exception thrown during LLM response generation.
     */
    private void handleLLMProcessingError(Throwable throwable) {
        String errorMessage = "\nLLM Response Error: " + throwable.getMessage();
        logger.error(errorMessage, throwable);
        updateOutput(errorMessage);
    }

    /**
     * Updates the UI output area safely on the Swing Event Dispatch Thread.
     * @param message The message to display in the output area.
     */
    private void updateOutput(String message) {
        SwingUtilities.invokeLater(() -> view.appendOutputText(message));
    }

    /**
     * Releases resources and disposes of any active RxJava subscriptions.
     * Should be called when the application is shut down.
     */
    public void dispose() {
        disposables.dispose();
    }

    /**
     * Main method to start the application.
     * Initializes the UI and model, and ensures resources are cleaned up on shutdown.
     * @param args Command-line arguments (not used).
     */
//    public static void main(String[] args) {
//        start();
//    }

    /**
     * Method to start the application.
     * Initializes the UI and model, and ensures resources are cleaned up on shutdown.
     */
    public static void start() {
        SwingUtilities.invokeLater(() -> {
            InputView view = new InputView();

            // Configure ASTAnalyzer to turn off analysis of specific parts of the AST
            ASTAnalyzerConfig config = new ASTAnalyzerConfig()
                    .analyzeClassFields(false)
                    .analyzeControlFlow(false)
                    .onlyAnalyzePublicMethods(true)
                    .omitPrivateMethodCalls(true);

            KnowledgeGraphService graphService = new KnowledgeGraphService(config);  // Initialize KnowledgeGraphService
            InputController controller = new InputController(view, graphService);

            // Ensure resources are disposed of when the application exits
            Runtime.getRuntime().addShutdownHook(new Thread(controller::dispose));
        });
    }
}
