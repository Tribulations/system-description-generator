package com.sdg.llm;

import com.sdg.logging.LoggerUtil;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * This class implements methods to generate high-level system descriptions and create
 * PlantUML diagrams by sending prompts to an LLM.
 * The prompt templates are defined in the {@link LLMPrompts} class.
 *
 * @see GeminiApiClient
 * @see ClaudeApiClient
 * @see LLMPrompts
 * @author Joakim Colloz
 * @version 1.3
 */
public class LLMService {
    private static final int MAX_TOKENS = 4096;
    private static final float TEMPERATURE = 0.0f;
    private final BaseClient client;

    public LLMService(final BaseClient client) {
        LoggerUtil.info(getClass(), "Initializing LLMService");
        this.client = client;
    }

    /**
     * Async helper to send a prompt, extract the answer, and optionally apply a post-action.
     */
    private CompletableFuture<String> sendPromptAsync(Supplier<String> promptSupplier, String logMessage,
                                                      Consumer<String> onAnswer) {
        LoggerUtil.info(getClass(), logMessage);
        try {
            final String prompt = promptSupplier.get();
            return client.sendRequestAsync(prompt, TEMPERATURE, MAX_TOKENS)
                    .thenApply(client::getAnswer)
                    .thenApply(answer -> {
                        if (onAnswer != null) {
                            onAnswer.accept(answer);
                        }
                        return answer;
                    });
        } catch (Exception e) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Async helper without a post-action.
     */
    private CompletableFuture<String> sendPromptAsync(Supplier<String> promptSupplier, String logMessage) {
        return sendPromptAsync(promptSupplier, logMessage, null);
    }

    /**
     * Synchronous helper to send a prompt, extract the answer, and optionally apply a post-action.
     */
    private String sendPromptSync(Supplier<String> promptSupplier, String logMessage, Consumer<String> onAnswer) {
        LoggerUtil.info(getClass(), logMessage);
        try {
            final String prompt = promptSupplier.get();
            final String response = client.sendRequest(prompt, TEMPERATURE, MAX_TOKENS);
            final String answer = client.getAnswer(response);
            if (onAnswer != null) {
                onAnswer.accept(answer);
            }
            return answer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a high-level description for a knowledge graph asynchronously.
     *
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return a CompletableFuture containing the generated high-level description
     */
    public CompletableFuture<String> generateHighLevelDescriptionAsync(String knowledgeGraphAsJson) {
        final String log = String.format(
                "Generating high-level description async for knowledge graph:%n%s%nusing prompt:%n%s",
                knowledgeGraphAsJson, LLMPrompts.PROMPT_TEMPLATE);
        return sendPromptAsync(() -> LLMPrompts.createPrompt(LLMPrompts.PROMPT_TEMPLATE, knowledgeGraphAsJson),
                log, this::writeDescriptionToFile);
    }

    public CompletableFuture<String> generatePlantUMLSyntaxAsync(final String highLevelDescription) {
        final String log = String.format(
                "Generating PlantUML syntax async for high-level description:%n%s%nusing prompt:%n%s",
                highLevelDescription, LLMPrompts.PLANT_UML_SYNTAX_TEMPLATE);
        return sendPromptAsync(() -> LLMPrompts.createPrompt(LLMPrompts.PLANT_UML_SYNTAX_TEMPLATE,
                highLevelDescription), log);
    }

    public CompletableFuture<String> tryCorrectPlantUMLSyntaxAsync(final String plantUMLSyntaxErrorMessage, final String plantUmlSyntax) {
        final String log = String.format(
                "Sending PlantUML syntax async to LLM for plant uml:%n%s%nusing prompt:%n%s",
                plantUmlSyntax, LLMPrompts.PLANT_UML_SYNTAX_CORRECTION_TEMPLATE);
        return sendPromptAsync(() -> LLMPrompts.createPrompt(LLMPrompts.PLANT_UML_SYNTAX_CORRECTION_TEMPLATE,
                        plantUMLSyntaxErrorMessage, plantUmlSyntax), log);
    }

    /**
     * Generates a high-level description for a knowledge graph using the Gemini API synchronously.
     *
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return the generated high-level description
     */
    public String generateHighLevelDescription(String knowledgeGraphAsJson) {
        final String log = String.format(
                "Generating high-level description for knowledge graph:%n%s%nusing prompt:%n%s",
                knowledgeGraphAsJson, LLMPrompts.PROMPT_TEMPLATE);
        return sendPromptSync(() -> LLMPrompts.createPrompt(LLMPrompts.PROMPT_TEMPLATE, knowledgeGraphAsJson),
                log, this::writeDescriptionToFile);
    }

    /**
     * Writes the generated description to a file.
     * @param description The description to write.
     */
    private void writeDescriptionToFile(String description) {
        final String filename = "description_output.txt";

        try {
            Files.writeString(Paths.get(filename), description, StandardOpenOption.CREATE);
        } catch (IOException e) {
            LoggerUtil.error(getClass(), "Failed to write generated description to file: " + filename, e);
        }
    }
}
