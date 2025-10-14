package com.sdg.diagrams;

import java.util.ArrayList;
import java.util.List;

public class PlantUMLDiagramExtractor {
    public static List<String> parsePlantUML(String plantUMLSyntax) { // TODO looks like the start directive has a leading space?
        final List<String> parsedUMLSyntaxList = new ArrayList<>();
        int startDirectiveIndex = 0, endDirectiveIndex = 0, traversedLength = 0;
        final String startDirective = "@startuml", endDirective = "@enduml";
        boolean firstStartDirectiveFound = false;

        for (final String currentLine : plantUMLSyntax.split("\n")) {
            if (currentLine.contains(startDirective)) {
                startDirectiveIndex = traversedLength;
                firstStartDirectiveFound = true;
            }


            if (currentLine.contains(endDirective)) {
                endDirectiveIndex = endDirective.length() + traversedLength;
            }

            if (startDirectiveIndex < endDirectiveIndex && firstStartDirectiveFound) {
                parsedUMLSyntaxList.add(plantUMLSyntax.substring(startDirectiveIndex, endDirectiveIndex));
                startDirectiveIndex = 0;
                endDirectiveIndex = 0;
            }

            // Add the current lines length + 1 so we take \n in the plantUMLSyntax into account
            traversedLength += currentLine.length() + 1;

        }

        return parsedUMLSyntaxList;
    }
}
