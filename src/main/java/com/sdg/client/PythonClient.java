package com.sdg.client;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PythonClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String URL = "http://localhost:5000";

    // TODO use RxJava in a suitable place for interaction with the Python micro
    // service!

    /**
     * Gets an answer from the LLM service.
     * 
     * @param prompt The prompt to send to the LLM
     * @param model  The model to use (one of: "bloom", "starcoder-3b",
     *               "starcoder-15b")
     * @return The answer from the LLM service as a JSONObject with a single
     *         "message" key containing the LLM answer as its value.
     * @throws Exception
     */
    public JSONObject llm(final String prompt, final String model) throws Exception {
        final String endpoint = URL + "/llm";

        JSONObject data = new JSONObject();
        data.put("prompt", prompt);
        data.put("model", model);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return new JSONObject(response.body());
    }

    /**
     * Simple test method to test the PythonClient class.
     * 
     * @param a
     * @param b
     * @return
     * @throws Exception
     */
    public JSONObject multiply(final int a, final int b) throws Exception {
        final String endpoint = URL + "/multiply";

        JSONObject data = new JSONObject();
        data.put("x", a);
        data.put("y", b);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        return new JSONObject(response.body());
    }
}
