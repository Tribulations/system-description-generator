package com.sdg.diagrams;

public class DiagramResult {
    private final String plantUMLSource; // null if failed
    private final String errorMessage;   // null if success

    private DiagramResult(String plantUMLSource, String errorMessage) {
        this.plantUMLSource = plantUMLSource;
        this.errorMessage = errorMessage;
    }

    public static DiagramResult success(String plantUMLSource) {
        return new DiagramResult(plantUMLSource, null);
    }

    public static DiagramResult failure(String errorMessage) {
        return new DiagramResult(null, errorMessage);
    }

    public boolean isSuccess() {
        return plantUMLSource != null;
    }

    public String getPlantUMLSource() {
        return plantUMLSource;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
