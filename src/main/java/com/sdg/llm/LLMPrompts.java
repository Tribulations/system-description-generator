package com.sdg.llm;

/**
 * This class provides prompts and a utility method for generating high-level descriptions and PlantUML syntax.
 * Used by the {@link LLMService}.
 *
 * @author Joakim Colloz
 * @version 1.1
 */
public class LLMPrompts {
    private LLMPrompts() {
        throw new IllegalStateException("Cannot instantiate this class");
    }

    public static final String PROMPT_TEMPLATE = """
                Given the following knowledge graph in JSON format representing Java software system at a high level:

                %s

                **Task:**
                Provide a **concise and structured high-level summary** of the system’s behavior and structure from the perspective of a new developer/maintainer being onboarded.
                Your response must include:
                1. **System Purpose:** Clearly state the system's primary function.
                2. **Key Components & Responsibilities:** Briefly describe the major components and their roles.
                3. **Core Technologies & Dependencies:** Explicitly confirm the technologies used and dependencies.
                4. **Architecture:** Provide a high-level overview of the system's architecture.
                5. **Data Flow:** Describe the flow of data between components.
                **Response Guidelines:**
                - Keep the summary **brief yet informative**.
                - Use **direct and factual statements** instead of speculation.
                - Ensure the response is structured and easy to understand.
                - **Avoid** using uncertain language like "might", "could", "may", etc.
                - Do not return your response as JSON.""";

    public static final String PLANT_UML_SYNTAX_TEMPLATE = """
            You are an expert software architect and PlantUML diagram generator.
            
            Based on the following high-level description of a software system, generate **five PlantUML diagrams** to help a new maintainer or developer quickly understand the system:
            
            1. **Use Case Diagram** – Show the primary actors (users, external systems) and their interactions with the system’s main features.
            2. **Component/Architecture Diagram** – Show the main system components (grouped into layers if applicable), their relationships, and dependencies.
            3. **Package Diagram** – Show the high-level organization of the codebase (Java packages, modules, or namespaces).
            4. **Sequence Diagram** – Illustrate a key workflow (choose the most representative or critical one, e.g., variant calling in GATK).
            5. **Activity Diagram** – Show the major steps and decision points in a core processing flow.
            
            For each diagram:
            - Use **PlantUML syntax**.
            - Include **titles and notes** where helpful to explain important parts.
            - Keep diagrams **high-level but accurate** – avoid too much detail that would overwhelm a new maintainer.   \s
            - Output all diagrams in separate PlantUML code blocks.
            - Make sure to not add information not included in the high-level textual description. 
            - Make sure to check for syntax errors.
            
            **System Description:**
            %s
            """;

    public static final String PLANT_UML_SYNTAX_CORRECTION_TEMPLATE = """
            Correct the following error:
            
            %s
            
            in the following Plant UML diagram:
            
            %s
            """;

    /**
     * Creates a prompt based on the provided prompt template and knowledge graph JSON.
     *
     * @param promptTemplate the prompt template. Preferably one of the constants in this class.
     * @param knowledgeGraphAsJson the knowledge graph in JSON format
     * @return the created prompt
     */
    public static String createPrompt(String promptTemplate, String knowledgeGraphAsJson) {
        return String.format(promptTemplate, knowledgeGraphAsJson);
    }
}
