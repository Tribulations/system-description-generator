package com.sdg.client;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class PythonClient {
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String URL = "http://localhost:5000/multiply";

    public String multiply(int x, int y) throws Exception {
        JSONObject data = new JSONObject();
        data.put("x", x);
        data.put("y", y);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(data.toString()))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        JSONObject result = new JSONObject(response.body());
        return result.getString("message");
    }
}
