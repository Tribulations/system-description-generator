package com.sdg.diagrams;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.stream.Stream;

import static com.sdg.diagrams.PlantUMLTestData.ONE_DIAGRAM_MISSING_END;
import static com.sdg.diagrams.PlantUMLTestData.ONE_DIAGRAM_MISSING_START;
import static com.sdg.diagrams.PlantUMLTestData.VALID_THREE_DIAGRAMS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlantUMLDiagramExtractorTest {
    @ParameterizedTest
    @ValueSource(strings = {
            VALID_THREE_DIAGRAMS,
            ONE_DIAGRAM_MISSING_START,
            ONE_DIAGRAM_MISSING_END
    })
    @DisplayName("Should ensure all parsed diagrams have correct start and end directives")
    void shouldEnsureAllParsedDiagramsHaveCorrectDirectives(String plantUMLDiagrams) {
        List<String> parsedPlantUMLList = PlantUMLDiagramExtractor.parsePlantUML(plantUMLDiagrams);

        for (String plantUMLDiagram : parsedPlantUMLList) {
            String[] lines = plantUMLDiagram.split("\n");
            String firstLine = lines[0];
            String lastLine = lines[lines.length - 1];

            assertEquals("@startuml", firstLine);
            assertEquals("@enduml", lastLine);
        }
    }

    @ParameterizedTest
    @MethodSource("provideDiagramTestCases")
    @DisplayName("Should parse valid diagrams and skip invalid ones or those with missing directives")
    void shouldParseValidDiagramsAndSkipInvalidOnes(String diagram, int expectedDiagramCount) {
        List<String> parsedPlantUMLList = PlantUMLDiagramExtractor.parsePlantUML(diagram);

        assertEquals(expectedDiagramCount, parsedPlantUMLList.size());
    }

    static Stream<Arguments> provideDiagramTestCases() {
        return Stream.of(
                Arguments.of(VALID_THREE_DIAGRAMS, 3),
                Arguments.of(ONE_DIAGRAM_MISSING_START, 2),
                Arguments.of(ONE_DIAGRAM_MISSING_END, 2),
                Arguments.of("invalid plant uml diagram syntax", 0)
        );
    }
}
