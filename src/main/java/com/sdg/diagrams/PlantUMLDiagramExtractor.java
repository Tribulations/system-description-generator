package com.sdg.diagrams;

import java.util.ArrayList;
import java.util.List;

public class PlantUMLDiagramExtractor {
    public static List<String> parsePlantUML(String plantUMLSyntax) {
        final List<String> diagrams = new ArrayList<>();
        final String[] lines = plantUMLSyntax.split("\n");

        boolean inside = false;
        StringBuilder current = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine;
            // Ignore markdown code fences
            if (line.startsWith("```") || line.startsWith("~~~")) {
                continue;
            }

            String trimmed = line.trim();
            boolean isStart = trimmed.startsWith("@startuml");
            boolean isEnd = trimmed.startsWith("@enduml");

            if (isStart) {
                inside = true;
                current.setLength(0);
                current.append("@startuml\n");
                continue;
            }

            if (inside) {
                if (isEnd) {
                    current.append("@enduml\n");
                    diagrams.add(current.toString());
                    inside = false;
                    current.setLength(0);
                } else {
                    current.append(line).append('\n');
                }
            }
        }

        return diagrams;
    }
}
