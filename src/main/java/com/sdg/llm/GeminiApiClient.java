package com.sdg.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * This class is a client for interacting with the Gemini API to retrieve answers from LLM.
 * To use this class, you need to have a valid API key and API URL.
 *
 * @see <a href="https://ai.google.dev/">Gemini API Documentation</a>
 * @version 1.1
 */
public class GeminiApiClient extends BaseClient {
    public GeminiApiClient(final String apiUrl, final String apiKey) {
        super(apiUrl, apiKey);
    }
    
    public GeminiApiClient() {
        this(GeminiApiConfig.API_URL, GeminiApiConfig.API_KEY);
    }

    /**
     * Sends a synchronous request to the Gemini API.
     *
     * @param prompt The user message to send to Gemini.
     * @param temperature The temperature parameter for controlling randomness.
     * @param maxTokens Maximum number of tokens in the response.
     * @return The API response as a JSON String.
     * @throws Exception If the request fails.
     */
    @Override
    public String sendRequest(final String prompt, final float temperature, final int maxTokens) throws Exception {
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
     * Sends an asynchronous request to the Gemini API.
     *
     * @param prompt The user message to send to Gemini.
     * @param temperature The temperature parameter for controlling randomness.
     * @param maxTokens Maximum number of tokens in the response.
     * @return A CompletableFuture containing the API response.
     */
    @Override
    public CompletableFuture<String> sendRequestAsync(final String prompt, final float temperature, final int maxTokens) {
        String requestBody = buildRequestBody(prompt, temperature, maxTokens);
        HttpRequest request = buildHttpRequest(requestBody);

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    } else {
                        throw new RuntimeException("API request failed with status code: " + response.statusCode() +
                                ", response: " + response.body());
                    }
                });
    }

    /**
     * Builds the JSON request body according to the Gemini API's expected format.
     *
     * @param prompt The user message to send.
     * @param temperature The temperature parameter for controlling randomness.
     * @param maxTokens Maximum number of tokens in the response.
     * @return A JSON string representing the request body.
     */
    @Override
    protected String buildRequestBody(final String prompt, final float temperature, final int maxTokens) {
        JSONObject requestBody = new JSONObject();
        JSONArray contentsArray = new JSONArray();
        JSONObject contentObject = new JSONObject();
        JSONArray partsArray = new JSONArray();
        JSONObject textPart = new JSONObject();

        textPart.put("text", prompt);
        partsArray.put(textPart);
        contentObject.put("parts", partsArray);
        contentsArray.put(contentObject);
        requestBody.put("contents", contentsArray);

        JSONObject generationConfig = new JSONObject();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", maxTokens);

        requestBody.put("generationConfig", generationConfig);

        return requestBody.toString();
    }

    /**
     * Builds the HTTP request with headers and request body.
     *
     * @param requestBody The JSON request body.
     * @return The constructed HttpRequest object.
     */
    @Override
    protected HttpRequest buildHttpRequest(String requestBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    /**
     * Extracts the generated text from the Gemini API response.
     *
     * @param response the response from the Gemini API
     * @return the extracted answer
     */
    @Override
    protected String getAnswer(String response) {
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
