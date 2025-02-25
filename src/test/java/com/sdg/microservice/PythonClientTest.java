package com.sdg.microservice;

import org.junit.jupiter.api.Test;

import com.sdg.client.PythonClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PythonClientTest {

    @Test
    void shouldMultiplyNumbersAndReturnCorrectResultString() throws Exception {
        PythonClient client = new PythonClient();
        String result = client.multiply(6, 7);
        assertEquals("The result of 6 * 7 is 42", result);
    }

    @Test
    void shouldReturnIncorrectResultString() throws Exception {
        PythonClient client = new PythonClient();
        String result = client.multiply(6, 7);
        assertNotEquals("The result of 2 * 7 is 42", result);
    }
}
