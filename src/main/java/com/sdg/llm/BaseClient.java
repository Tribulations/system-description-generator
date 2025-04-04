package com.sdg.llm;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public abstract  class BaseClient {
    protected final String apiUrl;
    protected final String apiKey;
    protected static final int connectionTimeoutSeconds = 30;

    protected final HttpClient httpClient;

    public BaseClient(final String apiUrl, final String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectionTimeoutSeconds))
                .build();
    }

    /**
     * Sends an asynchronous request to the API.
     *
     * @param prompt The user message to send to the API.
     * @return A CompletableFuture containing the API response.
     */
    public abstract CompletableFuture<String> sendRequestAsync(final String prompt, float temperature, final int maxTokens);

    /**
     * Sends a synchronous request to the API.
     *
     * @param prompt The user message to send to API.
     * @param temperature The temperature parameter for controlling randomness.
     * @param maxTokens Maximum number of tokens in the response.
     * @return The API response as a JSON String.
     * @throws Exception If the request fails.
     */
    public abstract String sendRequest(final String prompt, final float temperature, final int maxTokens) throws Exception;

    /**
     * Builds the JSON request body according to the Gemini API's expected format.
     *
     * @param prompt The user message to send.
     * @return A JSON string representing the request body.
     */
    protected abstract String buildRequestBody(final String prompt, final float temperature, final int maxTokens);

    /**
     * Builds the HTTP request with headers and request body.
     *
     * @param requestBody The JSON request body.
     * @return The constructed HttpRequest object.
     */
    protected abstract HttpRequest buildHttpRequest(String requestBody);

    protected abstract String getAnswer(String response);
}
