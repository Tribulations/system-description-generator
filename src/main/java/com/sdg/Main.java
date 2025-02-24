package com.sdg;

import com.sdg.graph.KnowledgeGraphService;
import com.sdg.graph.Neo4jDatabaseService;

public class Main {
        public static void main(String[] args) {
        try (Neo4jDatabaseService dbService = new Neo4jDatabaseService();
             KnowledgeGraphService graphService = new KnowledgeGraphService()) {

            // // Clean the database first
            dbService.deleteAllData();

            // // Insert the knowledge graph into the database
            graphService.insertToGraphDatabase("src/main/java/com/sdg/graph/TestClass.java");
        }
    }
}
