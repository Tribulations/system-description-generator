package com.sdg.diagrams;

import com.sdg.llm.LLMService;
import com.sdg.logging.LoggerUtil;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.Observable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DiagramFetcher {
    private final LLMService llmService;

    public DiagramFetcher(LLMService llmService) {
        this.llmService = llmService;
    }

    /**
     * Generates PlantUML diagrams from the given high level description.
     *
     * @param highLevelDescription the high level description of the system
     * @return a list of PlantUML diagrams (in plantUML syntax as strings)
     */
    public List<String> generatePlantUMLDiagrams(String highLevelDescription) throws InterruptedException {
        return generatePlantUMLDiagramsAsync(highLevelDescription).blockingGet();
    }

    public Single<List<String>> generatePlantUMLDiagramsAsync(String highLevelDescription) {
        return generatePlantUMLSyntaxAsync(highLevelDescription)
                .map(PlantUMLDiagramExtractor::parsePlantUML);
    }

    public Single<List<DiagramResult>> validateAndCorrectPlantUMLDiagramsWithResults(List<String> nonValidatedDiagrams) {
        // Validate each diagram; if invalid, correct via LLM and re-validate up to maxRetries.
        final int maxRetries = 2;

        return Observable.fromIterable(nonValidatedDiagrams)
                .concatMapSingle(diagram -> validateWithCorrections(diagram, maxRetries))
                .toList()
                .doOnSuccess(list -> LoggerUtil.debug(getClass(), "Validated (success or failure recorded) {} diagrams", list.size()));
    }

    // Returns only successfully validated diagrams
    public Single<List<String>> validateAndCorrectPlantUMLDiagrams(List<String> nonValidatedDiagrams) {
        return validateAndCorrectPlantUMLDiagramsWithResults(nonValidatedDiagrams)
                .map(list ->
                        Observable.fromIterable(list)
                                .filter(DiagramResult::isSuccess)
                                .map(DiagramResult::getPlantUMLSource)
                                .toList()
                                .blockingGet()
                );
    }

    /**
     * Attempts to validate a diagram; on failure, asks LLM to correct and re-validates up to retriesLeft times.
     */
    private Single<DiagramResult> validateWithCorrections(String diagram, int retriesLeft) {
        return validateOnce(diagram)
                .map(DiagramResult::success)
                .onErrorResumeNext(syntaxError -> {
                    LoggerUtil.debug(getClass(), "PlantUML syntax error: {}", syntaxError.getMessage());
                    if (retriesLeft <= 0) {
                        // After all retries, return a failure result instead of failing the stream.
                        String uiMessage = "Failed to generate this diagram. You can try to re-generate all diagrams again by pressing the Generate description button.";
                        return Single.just(DiagramResult.failure(uiMessage));
                    }
                    // Ask LLM to correct, then re-validate recursively with one fewer retry.
                    return tryCorrectPlantUMLSyntaxAsync(syntaxError.getMessage(), diagram)
                            .flatMap(corrected -> validateWithCorrections(corrected, retriesLeft - 1));
                });
    }

    /**
     * Validates a diagram once. Emits the original diagram on success and the plantuml error on invalid syntax.
     */
    private Single<String> validateOnce(String diagram) {
        return Single.fromCallable(() -> {
                    boolean validSyntax = PlantUMLValidator.validatePlantUMLSyntax(diagram);
                    if (!validSyntax) {
                        throw new IllegalStateException("Invalid PlantUML syntax");
                    }
                    LoggerUtil.debug(getClass(), "plant uml diagram validated successfully:\n {}", diagram);
                    return diagram;
                }
        );
    }

    public Single<String> generatePlantUMLSyntaxAsync(final String highLevelDescription) {
        return Single.create(emitter -> {
            // Get the CompletableFuture from the LLMService
            CompletableFuture<String> future = llmService.generatePlantUMLSyntaxAsync(highLevelDescription);

            // Handle success
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    emitter.onError(ex);  // Propagate error to RxJava
                } else {
                    emitter.onSuccess(result);  // Emit result to RxJava
                }
            });
        });
    }

    public Single<String> tryCorrectPlantUMLSyntaxAsync(final String plantUMLSyntaxErrorMessage, String plantUMLDiagrams) {
        return Single.create(emitter -> {
            // Get the CompletableFuture from the LLMService
            CompletableFuture<String> future = llmService.tryCorrectPlantUMLSyntaxAsync(plantUMLSyntaxErrorMessage,
                    plantUMLDiagrams);

            // Handle success
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    emitter.onError(ex);  // Propagate error to RxJava
                } else {
                    emitter.onSuccess(result);  // Emit result to RxJava
                }
            });
        });
    }

    public LLMService getLlmService() {
        return llmService;
    }
}
