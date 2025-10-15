package com.sdg.diagrams;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.sdg.diagrams.PlantUMLTestData.INVALID_PLANT_UML_SYNTAX_SOURCE;
import static com.sdg.diagrams.PlantUMLTestData.INVALID_UML;
import static com.sdg.diagrams.PlantUMLTestData.THROWS_ILLEGAL_STATE_EXCEPTION_BUT_SYNTAX_STILL_PASS;
import static com.sdg.diagrams.PlantUMLTestData.VALID_PLANT_UML_SYNTAX;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlantUMLValidatorTest {
    @Test
    void testIncorrectPlantUMLSyntaxThrowsException() {
        assertThrows(RuntimeException.class, () ->
                PlantUMLValidator.validatePlantUMLSyntax(INVALID_PLANT_UML_SYNTAX_SOURCE));
    }

    @Test
    void testIncorrectPlantUMLSyntaxThrowsException2() {
        assertThrows(RuntimeException.class, () ->
                PlantUMLValidator.validatePlantUMLSyntax(INVALID_UML));
    }

    @Test
    void testIncorrectPlantUMLSyntaxThrowsExceptionWithDescriptiveErrorMessage() throws IOException {
        String errorMessage = "";
        try {
            PlantUMLValidator.validatePlantUMLSyntax(INVALID_PLANT_UML_SYNTAX_SOURCE);
        } catch (RuntimeException e) {
            errorMessage = e.getMessage();
        }

        assertTrue(errorMessage.contains("Syntax Error"));
        assertTrue(errorMessage.contains("From string (line 7)"));
    }

    @Test
    void testCorrectPlantUMLSyntax() throws IOException {
        assertTrue(PlantUMLValidator.validatePlantUMLSyntax(VALID_PLANT_UML_SYNTAX));
    }

    /**
     * PlantUML library throws an IllegalArgumentException here even though the used PlantUML syntax seems correct.
     * This test was included to use when debugging PlantUMLValidator.
     */
    @Test
    void testCorrectPlantUMLSyntax2() throws IOException {
            assertTrue(PlantUMLValidator.validatePlantUMLSyntax(THROWS_ILLEGAL_STATE_EXCEPTION_BUT_SYNTAX_STILL_PASS));
    }
}
