package com.sdg.llm;

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

    public static final String PROMPT_TEMPLATE = """
                Given the following knowledge graph in JSON format representing Java software system at a high level:

                %s

                **Task:**
                Provide a **concise and structured high-level summary** of the system’s behavior and structure.
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
