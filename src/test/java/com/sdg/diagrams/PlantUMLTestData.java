package com.sdg.diagrams;

public final class PlantUMLTestData {
    private PlantUMLTestData() {
        throw new UnsupportedOperationException("Test data class cannot be instantiated");
    }

    public static final String VALID_THREE_DIAGRAMS = """
            @startuml
            some syntax
            @enduml
            
            @startuml
            ...
            ...
            @enduml
            
            @startuml
            ---
            ---
            @enduml""";

    public static final String ONE_DIAGRAM_MISSING_START = """
            some syntax
            @enduml
            
            @startuml
            ...
            @enduml
            
            @startuml
            ---
            @enduml""";

    public static final String ONE_DIAGRAM_MISSING_END = """
            @startuml
            some syntax
            
            @startuml
            ...
            @enduml
            
            @startuml
            ---
            @enduml""";
}
