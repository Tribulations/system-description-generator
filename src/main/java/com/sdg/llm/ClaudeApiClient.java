package com.sdg.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdg.logging.LoggerUtil;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * This class is a client for interacting with the Claude API to retrieve answers from LLM.
 * To use this class, you need to have a valid API key and API URL.
 *
 * Example response from Claude API:
 * {
 *   "id": "msg_019d5jMWks6Pv91t8hBh594m",
 *   "type": "message",
 *   "role": "assistant",
 *   "model": "claude-3-5-sonnet-20241022",
 *   "content": [
 *     {
 *       "type": "text",
 *       "text": "{\n  \"name\": \"John Smith\",\n  \"age\": 30,\n  \"city\": \"New York\",\n  \"email\": \"john@example.com\",\n  \"active\": true\n}"
 *     }
 *   ],
 *   "stop_reason": "end_turn",
 *   "stop_sequence": null,
 *   "usage": {
 *     "input_tokens": 17,
 *     "cache_creation_input_tokens": 0,
 *     "cache_read_input_tokens": 0,
 *     "output_tokens": 50
 *   }
 * }
 * @see <a href="https://docs.anthropic.com/en/home">Claude API Documentation</a>
 * @version 1.1
 * @author Joakim Colloz
 */
public class ClaudeApiClient extends BaseClient {
    private static final String API_VERSION = ClaudeApiConfig.API_VERSION;
    private static final String MODEL = ClaudeApiConfig.MODEL;

    public ClaudeApiClient(final String apiUrl, final String apiKey) {
        super(apiUrl, apiKey);
    }

    public ClaudeApiClient() {
        this(ClaudeApiConfig.API_URL, ClaudeApiConfig.API_KEY);
    }

    /**
     * Sends a synchronous request to the Claude API
     *
     * @param prompt The prompt to send to Claude
     * @param temperature The temperature parameter for controlling randomness
     * @param maxTokens Maximum number of tokens in the response
     * @return The API response as a JSON String
     * @throws Exception If the request fails
     */
    @Override
    public String sendRequest(String prompt, float temperature, int maxTokens) throws Exception {
        String requestBody = buildRequestBody(prompt, temperature, maxTokens);
        HttpRequest request = buildHttpRequest(requestBody);

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        } else {
            throw new RuntimeException("API request failed with status code: " + response.statusCode() +
                    ", response: " + response.body());
        }
    }

    /**
     * Sends an asynchronous request to the Claude API
     *
     * @param prompt The user message to send to Claude
     * @param maxTokens Maximum number of tokens in the response
     * @return A CompletableFuture containing the API response
     */
    public CompletableFuture<String> sendRequestAsync(String prompt, float temperature, int maxTokens) {
        String requestBody = buildRequestBody(prompt, temperature, maxTokens);
        HttpRequest request = buildHttpRequest(requestBody);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    } else {
                        LoggerUtil.error(getClass(), "API request failed with status code: " + response.statusCode());
                        throw new RuntimeException("API request failed with status code: " + response.statusCode() +
                                ", response: " + response.body());
                    }
                });
    }

    @Override
    protected String buildRequestBody(final String prompt, final float temperature, final int maxTokens) {
        return String.format("""
            {
                "model": "%s",
                "temperature": %f,
                "max_tokens": %d,
                "messages": [
                    {"role": "user", "content": "%s"}
                ]
            }
            """, MODEL, temperature, maxTokens, escapeJsonString(prompt));
    }

    @Override
    protected HttpRequest buildHttpRequest(String requestBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * Helper method to escape special characters in a JSON string.
     * @param input the input string to escape
     * @return the escaped string
     */
    private String escapeJsonString(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Helper method to extract the answer from a JSON response from the Claude API.
     *
     * @param response the response from the Claude API
     * @return the answer
     */
    @Override
    protected String getAnswer(String response) {
        JsonNode root;
        try {
            root = new ObjectMapper().readTree(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return root.get("content").get(0).get("text").asText();
    }
}
