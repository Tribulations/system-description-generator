package com.sdg.llm;

import com.sdg.logging.LoggerUtil;

import java.util.concurrent.CompletableFuture;

import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * This class provides methods to generate high-level descriptions for a knowledge graph using the Gemini API.
 * The prompts used to generate the descriptions are defined in the {@link LLMPrompts} class.
 *
 * @see GeminiApiClient
 * @version 1.2
 */
public class LLMService {
    private static final int MAX_TOKENS = 1024;
    private static final float TEMPERATURE = 0.0f;
    private final BaseClient client;

    public LLMService(final BaseClient client) {
        LoggerUtil.info(getClass(), "Initializing LLMService");
        this.client = client;
    }

    /**
     * Generates a high-level description for a knowledge graph using the Gemini API asynchronously.
     *
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return a CompletableFuture containing the generated high-level description
     */
    public CompletableFuture<String> generateHighLevelDescriptionAsync(String knowledgeGraphAsJson) {
        LoggerUtil.info(getClass(), "Generating high-level description async for knowledge graph: {}, using prompt: {}",
                knowledgeGraphAsJson, LLMPrompts.PROMPT_TEMPLATE);

        try {
            String prompt = LLMPrompts.createPrompt(LLMPrompts.PROMPT_TEMPLATE, knowledgeGraphAsJson);
            return client.sendRequestAsync(prompt, TEMPERATURE, MAX_TOKENS)
                    .thenApply(client::getAnswer)
                    .thenApply(answer -> {
                        writeDescriptionToFile(answer);
                        return answer;
                    });
        } catch (Exception e) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Generates a high-level description for a knowledge graph using the Gemini API synchronously.
     *
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return the generated high-level description
     */
    public String generateHighLevelDescription(String knowledgeGraphAsJson) {
        LoggerUtil.info(getClass(), "Generating high-level description for knowledge graph: {}, using prompt: {}",
                knowledgeGraphAsJson, LLMPrompts.PROMPT_TEMPLATE);

        String response;
        try {
            String prompt = LLMPrompts.createPrompt(LLMPrompts.PROMPT_TEMPLATE, knowledgeGraphAsJson);
            response = client.sendRequest(prompt, TEMPERATURE, MAX_TOKENS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String answer = client.getAnswer(response);
        writeDescriptionToFile(answer);
        return answer;
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
