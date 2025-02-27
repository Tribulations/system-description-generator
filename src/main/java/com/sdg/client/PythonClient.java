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

    // TODO handle error we get when microservice not running

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

    public static void main(String[] args) throws Exception {
        testLlm();
    }

    /**
     * Pass a prompt to the LLM service and measure the time it takes to get a
     * response.
     * 
     * The src/main/python/microservice/service.py must be running.
     * 
     * Received response after a test run of this method:
     * UserAuthenticationModule is a component that handles user authentication and
     * authorization. It interacts with the DatabaseConnector, LoggingService,
     * TokenManager, AuditSystem, and NotificationService components. It implements
     * the OAuth2.0Protocol and TwoFactorAuthentication protocols. It sends events
     * to the AuditSystem and NotificationService components. It has the
     * ConfigurationManager, EncryptionLibrary, and DatabaseConnector dependencies.
     * 
     * @throws Exception
     */
    private static void testLlm() throws Exception {
        PythonClient pythonClient = new PythonClient();
        long start = System.currentTimeMillis();

        // System.out.println(pythonClient.llm("Is USA a country?",
        // "bloom").getString("message"));

        String topLevelNode = """
                {
                  "UserAuthenticationModule": {
                    "calls": ["DatabaseConnector", "LoggingService", "TokenManager"],
                    "implements": ["OAuth2.0Protocol", "TwoFactorAuthentication"],
                    "sends_events_to": ["AuditSystem", "NotificationService"],
                    "dependencies": ["ConfigurationManager", "EncryptionLibrary"]
                  }
                }
                """;

        String promptTemplate = """
                Given the following knowledge graph in JSON format:

                %s

                Provide a short high-level description of this component, explaining:
                1. What other components it interacts with
                2. What protocols it implements
                3. What events it triggers
                4. What dependencies it has
                5. Its likely overall responsibility in the system architecture

                Description:
                """;

        String finalPrompt = String.format(promptTemplate, topLevelNode);

        System.out.println(pythonClient.llm(finalPrompt,
                "starcoder-3b").getString("message"));

        long end = System.currentTimeMillis();
        System.out.println("Time taken: " + ((end - start) / 1000) + " seconds");
    }
}
