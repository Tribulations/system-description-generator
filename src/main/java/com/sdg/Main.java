package com.sdg;

import com.sdg.client.PythonClient;
import com.sdg.graph.KnowledgeGraphService;

public class Main {
    public static void main(String[] args) throws Exception {
        // knowledgeGraphServiceTest();
        PythonClient pythonClient = new PythonClient();
        System.out.println(pythonClient.llm("What is the capital of France?"));
    }

    private static void knowledgeGraphServiceTest() {
        try (KnowledgeGraphService graphService = new KnowledgeGraphService()) {

            // // Clean the database first
            graphService.deleteAllData();

            // // Insert the knowledge graph into the database
            graphService.insertToGraphDatabase("src/main/java/com/sdg/graph/TestClass.java");
        }
    }
}
