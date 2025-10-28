package com.sdg.controller;

import com.sdg.ast.ASTAnalyzerConfig;
import com.sdg.diagrams.DiagramFetcher;
import com.sdg.diagrams.DiagramResult;
import com.sdg.diagrams.DiagramTabManager;
import com.sdg.graph.KnowledgeGraphService;
import com.sdg.io.FileIO;
import com.sdg.io.OutputPaths;

import com.sdg.llm.LLMClient;
import com.sdg.llm.ClaudeApiClient;
import com.sdg.llm.GeminiApiClient;
import com.sdg.llm.LLMService;
import com.sdg.logging.EnvConfig;
import com.sdg.logging.LoggerUtil;
import com.sdg.model.InputHandler.ProcessingResult;
import com.sdg.view.MainView;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.core.Scheduler;

import javax.swing.SwingUtilities;
import javax.swing.JFileChooser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;

/**
 * The InputController class handles user interactions in the UI, processes input files,
 * and manages asynchronous operations using RxJava.
 *
 * @author Suraj Karki
 * @author Joaim Colloz
 */
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private final MainView view;
    private final KnowledgeGraphService graphService;
    private final DiagramFetcher diagramFetcher;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Scheduler swingScheduler = Schedulers.from(SwingUtilities::invokeLater);

    // If neither of these two models is used, a delay is added between LLM requests
    // to prevent exceeding the model’s maximum token rate limit.
    private static final String GEMINI_DEFAULT_MODEL = "gemini-2.0-flash";
    private static final String CLAUDE_DEFAULT_MODEL = "claude-3-5-sonnet-20241022";

    /**
     * Constructor for InputController.
     * @param view  The UI component for user interaction.
     * @param graphService The service responsible for processing files and generating the knowledge graph.
     */
    public MainController(MainView view, KnowledgeGraphService graphService, DiagramFetcher diagramFetcher) {
        this.view = view;
        this.graphService = graphService;
        this.diagramFetcher = diagramFetcher;

        // Attach event listeners to the UI buttons
        view.addBrowseListener(e -> browseFile());
        view.addProcessListener(e -> processInput());
        view.addDecListener(e -> generateTextualDescription());
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
                        .doOnSubscribe(disposable -> view.showLoadingIndicator("Processing..."))
                        .doFinally(() -> {
                            view.setOutputText("Knowledge Graph created. " +
                                    "You can generate the descriptions now.");
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
    private void generateTextualDescription() {
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

        String filepath = "textual_description.txt";
        try {
            writeToFile(response, filepath);
        } catch (IOException e) {
            LoggerUtil.error(getClass(), "Failed to write generated description to file: " + filepath, e);
        }

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
        final int delay = isDefaultLLMModel() ? 1100 : 62000;
        LoggerUtil.info(getClass(), "Delaying for {} ms before sending diagram generation prompt to LLM to not exceed token rate limit", delay);

        disposables.add(
                Completable.timer(delay, TimeUnit.MILLISECONDS, Schedulers.io()) // Delay request to LLM to not exceed the input token rate limit
                        .andThen(diagramFetcher.generatePlantUMLDiagramsAsync(systemDescription))
                        .subscribeOn(Schedulers.io())
                        .flatMap(diagramFetcher::validateAndCorrectPlantUMLDiagramsWithResults)
                        .observeOn(swingScheduler)
                        .doFinally(view::clearLoadingIndicator)
                        .subscribe(this::handleGeneratedDiagrams, this::handleLLMProcessingError)
        );
    }

    private boolean isDefaultLLMModel() {
        final String modelName = diagramFetcher.getLlmService().getLLMModelName();

        return modelName.equals(GEMINI_DEFAULT_MODEL) || modelName.equals(CLAUDE_DEFAULT_MODEL);
    }

    /**
     * Handles the generated diagrams by logging them and updating the UI.
     *
     * @param validatedDiagrams The list of validated PlantUML diagrams
     */
    private void handleGeneratedDiagrams(List<DiagramResult> validatedDiagrams) {
        int index = 1;

        for (DiagramResult result : validatedDiagrams) {

            if (result.isSuccess()) {
                LoggerUtil.info(getClass(), "Generated diagram: \n" + result.getPlantUMLSource());

                String filename = "diagram" + index++ + ".puml";
                try {
                    writeToFile(result.getPlantUMLSource(), filename);
                } catch (IOException e) {
                    LoggerUtil.error(getClass(), "Failed to write generated PlantUml to file: " + filename, e);
                }
            } else {
                LoggerUtil.info(getClass(), "Diagram generation failed with message: {}", result.getErrorMessage());
            }
        }
        DiagramTabManager manager = new DiagramTabManager(validatedDiagrams, true);
        view.addDiagramToTabbedPane("UML Diagrams", manager);

        // Auto-save successfully generated diagrams in background to avoid blocking UI
        disposables.add(
                Completable.fromRunnable(() -> saveDiagrams(graphService.getSystemName(), manager))
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                () -> LoggerUtil.info(getClass(), "Diagrams saved successfully."),
                                err -> LoggerUtil.error(getClass(), "Failed to save diagrams: {}", err.getMessage())
                        )
        );
    }

    private void writeToFile(String data, String filename) throws IOException {
        Path outputDir = OutputPaths.getBaseOutputDir(graphService.getSystemName());
        Path outputPath = outputDir.resolve(filename);
        FileIO.write(outputPath, data);
        LoggerUtil.info(getClass(), "Wrote file to {}", outputPath.toAbsolutePath());
    }

    private void saveDiagrams(final String systemName, DiagramTabManager manager) {
        try {
            Path outputDir = OutputPaths.getDiagramsDir(systemName);
            Files.createDirectories(outputDir);
            manager.saveAllDiagramsAsPng(outputDir);
            LoggerUtil.info(getClass(), "Saved diagrams to {}", outputDir.toAbsolutePath());
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "Error saving diagrams: {}", e.getMessage());
        }
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

            LLMClient llmClient = createLLMClient();
            LLMService llmService = new LLMService(llmClient);
            KnowledgeGraphService graphService = new KnowledgeGraphService(config, llmClient);
            DiagramFetcher diagramFetcher = new DiagramFetcher(llmService);

            MainController controller = new MainController(view, graphService, diagramFetcher);

            // Ensure resources are disposed of when the application exits
            Runtime.getRuntime().addShutdownHook(new Thread(controller::dispose));
        });
    }

    private static LLMClient createLLMClient() {
        if (EnvConfig.getInstance().getOrDefault("GEMINI_API_KEY", "").isEmpty())  {
            return new ClaudeApiClient();
        } else {
            return new GeminiApiClient();
        }
    }
}
