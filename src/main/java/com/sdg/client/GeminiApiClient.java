package com.sdg.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.json.JSONObject;
import org.json.JSONArray;

/**
 * This class is a client for interacting with the Gemini API to retrieve answers from LLM.
 * To use this class, you need to have a valid API key and API URL.
 *
 * @see <a href="https://ai.google.dev/">Gemini API Documentation</a>
 * @version 1.1
 */

public class GeminiApiClient {
    private static final String API_URL = GeminiApiConfig.API_URL;
    private static final String API_KEY = GeminiApiConfig.API_KEY;

    private final HttpClient httpClient;

    public GeminiApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Sends a synchronous request to the Gemini API.
     *
     * @param prompt The user message to send to Gemini.
     * @return The API response as a JSON String.
     * @throws Exception If the request fails.
     */
    public String sendRequest(String prompt) throws Exception {
        String requestBody = buildRequestBody(prompt);
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
     * @return A CompletableFuture containing the API response.
     */
    public CompletableFuture<String> sendRequestAsync(String prompt) {
        String requestBody = buildRequestBody(prompt);
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
     * @return A JSON string representing the request body.
     */
    private String buildRequestBody(String prompt) {
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

        return requestBody.toString();
    }

    /**
     * Builds the HTTP request with headers and request body.
     *
     * @param requestBody The JSON request body.
     * @return The constructed HttpRequest object.
     */
    private static HttpRequest buildHttpRequest(String requestBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?key=" + API_KEY))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }
}