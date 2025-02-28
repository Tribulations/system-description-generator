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
    private final PythonClient client = new PythonClient();

    /**
     * Simple initial test to test the PythonClient class.
     * 
     * @throws Exception
     */
    @Test
    void shouldMultiply() throws Exception {
        JSONObject result = client.multiply(2, 3);

        assertEquals("The result of 2 * 3 is 6", result.getString("message"));
        assertNotEquals("This string is not the result", result.getString("message"));
    }

    /**
     * Tests that the LLM service returns a JSONObject with a "message" key.
     * Uses the bloom model. This test should go pretty fast.
     * 
     * @throws Exception
     */
    @Test
    void testBloomModel() throws Exception {
        String prompt = "What is the capital of France?";
        JSONObject result = client.llm(prompt, "bloom");

        assertTrue(result.has("message"));

        String unPredictableAnswer = result.getString("message");
        assertFalse(unPredictableAnswer.isEmpty());
    }

    /**
     * Tests that the LLM service returns a JSONObject with a "message" key.
     * Uses the starcoder-3b model. This test can take up to a minute to complete.
     * 
     * @throws Exception
     */
    @Test
    void testStarCoder3bModel() throws Exception {
        String prompt = "What is Python?";
        JSONObject result = client.llm(prompt, "starcoder-3b");

        assertTrue(result.has("message"));

        String unPredictableAnswer = result.getString("message");
        assertFalse(unPredictableAnswer.isEmpty());
    }

    /**
     * Tests that the LLM service returns a JSONObject with a "message" key.
     * Uses the starcoder-15b model. This test can take a while to run (a couple of
     * minutes or more). Also uses a lot of RAM.
     * 
     * @throws Exception
     */
    @Test
    void testStarCoder15bModel() throws Exception {
        String prompt = "What is Python?";
        JSONObject result = client.llm(prompt, "starcoder-15b");

        assertTrue(result.has("message"));

        String unPredictableAnswer = result.getString("message");
        assertFalse(unPredictableAnswer.isEmpty());
    }
}
