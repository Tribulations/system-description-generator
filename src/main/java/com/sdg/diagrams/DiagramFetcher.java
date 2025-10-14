package com.sdg.diagrams;

import com.sdg.llm.LLMService;
import com.sdg.logging.LoggerUtil;
import io.reactivex.rxjava3.core.Single;

import java.util.ArrayList;
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
        Thread.sleep(2000); // TODO fix this

        return generatePlantUMLSyntaxAsync(highLevelDescription)
                .flatMap(plantUMLDiagrams ->
                        Single.just(PlantUMLDiagramExtractor.parsePlantUML(plantUMLDiagrams)))
                .blockingGet();
    }

    public Single<List<String>> validateAndCorrectPlantUMLDiagrams(List<String> nonValidatedDiagrams) {
        // Check if diagram syntax is correct. If not, send to llm again for correction
        List<String> validatedDiagrams = new ArrayList<>();
        for (String diagram : nonValidatedDiagrams) {
            final int maxRetries = 2;
            int retries = 0;
            boolean validSyntax = false; // we need to get the error message from validator
            String errorMessage = "";
            while ((!validSyntax && retries <= maxRetries)) {
                try {
                    Thread.sleep(3000);
                    validSyntax = PlantUMLValidator.validatePlantUMLSyntax(diagram);

                    if (validSyntax) {
                        LoggerUtil.debug(getClass(),
                                "plant uml diagram validated successfully:\n {}", diagram);
                        validatedDiagrams.add(diagram);
                    } else {
                        LoggerUtil.debug(getClass(),
                                "Performed {} retries without success for plant uml diagram:\n {}",
                                retries, diagram);
                    }
                } catch (Exception e) {
                    // the error message from validator is used with the prompt sent to LLM for correction
                    LoggerUtil.debug(getClass(), "PlantUML syntax error: {}", e.getMessage());
                    System.out.println(e.getMessage());
                    errorMessage = e.getMessage();
                    diagram = tryCorrectPlantUMLSyntaxAsync(errorMessage, diagram).toString(); // TODO we should maybe have a delay here
                    retries++;
                }
            }
        }

        return Single.just(validatedDiagrams);
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
            CompletableFuture<String> future = llmService.tryCorrectPlantUMLSyntaxAsync(
                    plantUMLSyntaxErrorMessage, plantUMLDiagrams);

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
}
