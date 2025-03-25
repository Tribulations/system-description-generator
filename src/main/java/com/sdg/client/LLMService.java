package com.sdg.client;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.sdg.logging.LoggerUtil;
//
//import java.util.concurrent.CompletableFuture;
//
///**
// * This class provides methods to generate high-level descriptions for a knowledge graph using Claude API.
// * The prompts used to generate the descriptions are defined in the {@link LLMPrompts} class.
// *
// * @see ClaudeApiClient
// * @author Joakim Colloz
// * @version 1.0
// */
//public class LLMService {
//    private static final int MAX_TOKENS = 1024;
//    private final ClaudeApiClient client = new ClaudeApiClient();
//
//    /**
//     * Generates a high-level description for a knowledge graph using Claude API asynchronously.
//     *
//     * @param knowledgeGraphAsJson the knowledge graph in JSON format
//     * @return the generated high-level description
//     */
//    public CompletableFuture<String> generateHighLevelDescriptionAsync(String knowledgeGraphAsJson) {
//        LoggerUtil.info(getClass(), "Generating high level description for knowledge graph: {}, using prompt: {}",
//                knowledgeGraphAsJson, LLMPrompts.promptTemplate3);
//
//        try {
//            String prompt = LLMPrompts.createPrompt(LLMPrompts.promptTemplate3, knowledgeGraphAsJson);
//            return client.sendRequestAsync(prompt, MAX_TOKENS)
//                    .thenApply(LLMService::getAnswer);
//        } catch (Exception e) {
//            CompletableFuture<String> future = new CompletableFuture<>();
//            future.completeExceptionally(e);
//            return future;
//        }
//    }
//
//    /**
//     * Generates a high-level description for a knowledge graph using Claude API synchronously.
//     *
//     * @param knowledgeGraphAsJson the knowledge graph in JSON format
//     * @return the generated high-level description
//     */
//    public String generateHighLevelDescription(String knowledgeGraphAsJson) {
//        LoggerUtil.info(getClass(), "Generating high level description for knowledge graph: {}, using prompt: {}",
//                knowledgeGraphAsJson, LLMPrompts.promptTemplate3);
//
//        String response;
//        try {
//            String prompt = LLMPrompts.createPrompt(LLMPrompts.promptTemplate3, knowledgeGraphAsJson);
//            response = client.sendRequest(prompt, MAX_TOKENS);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//
//        return getAnswer(response);
//    }
//
//    /**
//     * Helper method to extract the answer from a JSON response from the Claude API.
//     *
//     * @param response the response from the Claude API
//     * @return the answer
//     */
//    private static String getAnswer(String response) {
//        JsonNode root;
//        try {
//            root = new ObjectMapper().readTree(response);
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//        return root.get("content").get(0).get("text").asText();
//    }
//}

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdg.logging.LoggerUtil;

import java.util.concurrent.CompletableFuture;

/**
 * This class provides methods to generate high-level descriptions for a knowledge graph using the Gemini API.
 * The prompts used to generate the descriptions are defined in the {@link LLMPrompts} class.
 *
 * @see GeminiApiClient
 * @version 1.1
 */
public class LLMService {
    private final GeminiApiClient client = new GeminiApiClient();

    /**
     * Generates a high-level description for a knowledge graph using the Gemini API asynchronously.
     *
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return a CompletableFuture containing the generated high-level description
     */
    public CompletableFuture<String> generateHighLevelDescriptionAsync(String knowledgeGraphAsJson) {
        LoggerUtil.info(getClass(), "Generating high-level description for knowledge graph: {}, using prompt: {}",
                knowledgeGraphAsJson, LLMPrompts.promptTemplate3);

        try {
            String prompt = LLMPrompts.createPrompt(LLMPrompts.promptTemplate3, knowledgeGraphAsJson);
            return client.sendRequestAsync(prompt)
                    .thenApply(LLMService::getAnswer);
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
                knowledgeGraphAsJson, LLMPrompts.promptTemplate3);

        String response;
        try {
            String prompt = LLMPrompts.createPrompt(LLMPrompts.promptTemplate3, knowledgeGraphAsJson);
            response = client.sendRequest(prompt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return getAnswer(response);
    }

    /**
     * Extracts the generated text from the Gemini API response.
     *
     * @param response the response from the Gemini API
     * @return the extracted answer
     */
    private static String getAnswer(String response) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");

            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");

                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse response: " + e.getMessage(), e);
        }

        throw new RuntimeException("Unexpected API response format: " + response);
    }
}
