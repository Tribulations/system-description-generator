package com.sdg.microservice;

import com.sdg.client.PythonClient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.json.JSONObject;

/**
 * Make sure to start the LLM service in file python/microservice/service.py
 * before running this test.
 */
class PythonClientTest {
    /**
     * Tests that the LLM service returns a JSONObject with a "message" key.
     * 
     * @throws Exception
     */
    @Test
    void shouldGetJSONObjectFromLlmWithMessageKey() throws Exception {
        PythonClient client = new PythonClient();
        JSONObject result = client.llm("What is the capital of France?");
        assertTrue(result.has("message"));
        assertFalse(result.has("non-existant-key"));
    }

    /**
     * Simple initial test to test the PythonClient class.
     * 
     * @throws Exception
     */
    @Test
    void shouldMultiply() throws Exception {
        PythonClient client = new PythonClient();
        JSONObject result = client.multiply(2, 3);
        assertEquals("The result of 2 * 3 is 6", result.getString("message"));
        assertNotEquals("This string is not the result", result.getString("message"));
    }
}
