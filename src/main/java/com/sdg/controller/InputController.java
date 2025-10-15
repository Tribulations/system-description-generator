package com.sdg.controller;

import com.sdg.ast.ASTAnalyzerConfig;
import com.sdg.diagrams.DiagramFetcher;
import com.sdg.diagrams.DiagramTabManager;
import com.sdg.graph.KnowledgeGraphService;

import com.sdg.llm.GeminiApiClient;
import com.sdg.llm.LLMService;
import com.sdg.logging.LoggerUtil;
import com.sdg.model.InputHandler.ProcessingResult;
import com.sdg.view.MainView;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.core.Scheduler;

import javax.swing.SwingUtilities;
import javax.swing.JFileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The InputController class handles user interactions in the UI, processes input files,
 * and manages asynchronous operations using RxJava.
 *
 * @author Suraj Karki
 * @author Joaim Colloz
 */
public class InputController {
    private static final Logger logger = LoggerFactory.getLogger(InputController.class);

    private final MainView view;
    private final KnowledgeGraphService graphService;
    private final DiagramFetcher diagramFetcher;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Scheduler swingScheduler = Schedulers.from(SwingUtilities::invokeLater);

    /**
     * Constructor for InputController.
     * @param view  The UI component for user interaction.
     * @param graphService The service responsible for processing files and generating the knowledge graph.
     */
    public InputController(MainView view, KnowledgeGraphService graphService, DiagramFetcher diagramFetcher) {
        this.view = view;
        this.graphService = graphService;
        this.diagramFetcher = diagramFetcher;

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
        view.clearOutputArea();

        if (result == JFileChooser.APPROVE_OPTION) {
            String selectedPath = fileChooser.getSelectedFile().getAbsolutePath();
            //updateOutput("Selected: " + selectedPath);
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
        if (inputPath.isEmpty() || inputPath.equals("Browse your project / Enter GitHub url...")) {
            updateOutput("Error: No input path provided.");
            return;
        }

        view.getProcessButton().setEnabled(false);
        view.getDescButton().setEnabled(true);

        // Subscribe to the RxJava observable processing files asynchronously
        final boolean resetDatabase = true;
        disposables.add(
                graphService.processKnowledgeGraph(inputPath, resetDatabase)
                        .subscribeOn(Schedulers.io())
                        .observeOn(swingScheduler)
                        .doOnSubscribe(disposable -> view.showLoadingIndicator("Processing " +
                                "files..."))
                        .doFinally(() -> {
                            view.setOutputText("Knowledge Graph created. " +
                                    "You can generate the description now.");
                            view.clearLoadingIndicator(); // Clear loading indicator after processing
                        })
                        .subscribe(this::logProcessingResult, this::handleProcessingError)
        );
    }

    /**
     * Handles successful file processing results.
     * Updates the UI with details of the processed file.
     * @param result The processing result containing file details and content.
     */
    private void logProcessingResult(ProcessingResult result) {
        LoggerUtil.info(getClass(), "Project directory processed successfully: {}", result.file());
    }

    /**
     * Handles errors that occur during file processing.
     * Logs the error and displays a message in the UI.
     * @param throwable The exception thrown during processing.
     */
    private void handleProcessingError(Throwable throwable) {
        String errorMessage = "\nError: " + throwable.getMessage();
        view.clearLoadingIndicator();
        logger.error(errorMessage, throwable);
        updateOutput(errorMessage);
    }

    /**
     * Triggers LLM response generation and updates the UI with the generated response.
     */
    private void generateDescription() {
        view.getProcessButton().setEnabled(true);
        view.clearOutputArea();
        disposables.add(
                graphService.generateLLMResponseAsync()
                        .subscribeOn(Schedulers.io())   // Run on a background thread
                        .observeOn(swingScheduler)     // UI updates on Swing thread
                        .doOnSubscribe(disposable -> view.showLoadingIndicator("Generating textual description..."))
                        .subscribe(
                                this::handleLLMResponse,
                                this::handleLLMProcessingError
                        )
        );
    }

    /**
     * Handles the LLM response by updating the UI and triggering diagram generation.
     *
     * @param response The response from the LLM containing the system description
     */
    private void handleLLMResponse(String response) {
        updateOutput("\nSystem Description: " + response);
        LoggerUtil.info(getClass(), "Received system description: \n" + response);

        // Generate and validate diagrams asynchronously to avoid blocking the UI
        // Switch the loading indicator to the diagrams phase and let that stream clear it when done
        view.showLoadingIndicator("Generating diagrams...");
        generateAndDisplayDiagrams(response);
    }

    /**
     * Generates and displays PlantUML diagrams based on the system description.
     *
     * @param systemDescription The system description to generate diagrams from
     */
    private void generateAndDisplayDiagrams(String systemDescription) {
        disposables.add(
                diagramFetcher.generatePlantUMLDiagramsAsync(systemDescription)
                        .subscribeOn(Schedulers.io())
                        .flatMap(diagramFetcher::validateAndCorrectPlantUMLDiagrams)
                        .observeOn(swingScheduler)
                        .doFinally(view::clearLoadingIndicator)
                        .subscribe(this::handleGeneratedDiagrams, this::handleLLMProcessingError)
        );
    }

    /**
     * Handles the generated diagrams by logging them and updating the UI.
     *
     * @param validatedDiagrams The list of validated PlantUML diagrams
     */
    private void handleGeneratedDiagrams(List<String> validatedDiagrams) {
        for (String diagram : validatedDiagrams) {
            LoggerUtil.info(getClass(), "Generated diagram: \n" + diagram);
        }
        view.addDiagramToTabbedPane("UML Diagrams", new DiagramTabManager(validatedDiagrams));
    }

    /**
     * Handles errors that occur during LLM response generation.
     * Logs the error and displays a message in the UI.
     * @param throwable The exception thrown during LLM response generation.
     */
    private void handleLLMProcessingError(Throwable throwable) {
        String errorMessage = "\nLLM Response Error: " + throwable.getMessage();
        view.clearLoadingIndicator();
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
     * Method to start the application.
     * Initializes the UI and model, and ensures resources are cleaned up on shutdown.
     */
    public static void start() {
        SwingUtilities.invokeLater(() -> {
            MainView view = new MainView();

            // Configure ASTAnalyzer to turn off analysis of specific parts of the AST
            ASTAnalyzerConfig config = new ASTAnalyzerConfig()
                    .analyzeClassFields(false)
                    .analyzeControlFlow(false)
                    .onlyAnalyzePublicMethods(true)
                    .omitPrivateMethodCalls(true);

            KnowledgeGraphService graphService = new KnowledgeGraphService(config);  // Initialize KnowledgeGraphService
            DiagramFetcher diagramFetcher = new DiagramFetcher(new LLMService(new GeminiApiClient()));
            InputController controller = new InputController(view, graphService, diagramFetcher);

            // Ensure resources are disposed of when the application exits
            Runtime.getRuntime().addShutdownHook(new Thread(controller::dispose));
        });
    }
}
