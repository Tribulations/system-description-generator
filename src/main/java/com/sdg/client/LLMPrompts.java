package com.sdg.client;

/**
 * This class provides prompts and utility methods for generating high-level descriptions.
 * Used by the {@link LLMService} class to generate descriptions.
 *
 * @author Joakim Colloz
 * @version 1.0
 */
public class LLMPrompts {
    private LLMPrompts() {
        throw new IllegalStateException("Cannot instantiate this class");
    }

    public static final String promptTemplate1 = """
                Given the following knowledge graph in JSON format representing a component in a software system:

                %s

                Provide a short high-level description of this component, explaining:
                1. What other components it interacts with
                2. What dependencies it has
                3. Its likely overall responsibility in the system architecture
                """;

    public static final String promptTemplate2 = """
                Given the following knowledge graph in JSON format representing a software system at a high level:

                %s

                Provide a short high-level description of this systems behavior.
                """;


    public static final String promptTemplate3 = """
                Given the following knowledge graph in JSON format representing a software system at a high level:

                %s

                Provide a short and coherent high-level description of this systems behavior.
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
