package com.sdg.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdg.logging.LoggerUtil;

import java.util.concurrent.CompletableFuture;

/**
 * This class provides methods to generate high-level descriptions for a knowledge graph using Claude API.
 * The prompts used to generate the descriptions are defined in the {@link LLMPrompts} class.
 *
 * @see ClaudeApiClient
 * @author Joakim Colloz
 * @version 1.0
 */
public class LLMService {
    private static final int MAX_TOKENS = 1024;
    private final ClaudeApiClient client = new ClaudeApiClient();

    /**
     * Generates a high-level description for a knowledge graph using Claude API asynchronously.
     *
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return the generated high-level description
     */
    public CompletableFuture<String> generateHighLevelDescriptionAsync(String knowledgeGraphAsJson) {
        LoggerUtil.info(getClass(), "Generating high level description for knowledge graph: {}, using prompt: {}",
                knowledgeGraphAsJson, LLMPrompts.promptTemplate3);

        try {
            String prompt = LLMPrompts.createPrompt(LLMPrompts.promptTemplate3, knowledgeGraphAsJson);
            return client.sendRequestAsync(prompt, MAX_TOKENS)
                    .thenApply(LLMService::getAnswer);
        } catch (Exception e) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Generates a high-level description for a knowledge graph using Claude API synchronously.
     *
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return the generated high-level description
     */
    public String generateHighLevelDescription(String knowledgeGraphAsJson) {
        LoggerUtil.info(getClass(), "Generating high level description for knowledge graph: {}, using prompt: {}",
                knowledgeGraphAsJson, LLMPrompts.promptTemplate3);

        String response;
        try {
            String prompt = LLMPrompts.createPrompt(LLMPrompts.promptTemplate3, knowledgeGraphAsJson);
            response = client.sendRequest(prompt, MAX_TOKENS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return getAnswer(response);
    }

    /**
     * Helper method to extract the answer from a JSON response from the Claude API.
     *
     * @param response the response from the Claude API
     * @return the answer
     */
    private static String getAnswer(String response) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return root.get("content").get(0).get("text").asText();
    }
}
