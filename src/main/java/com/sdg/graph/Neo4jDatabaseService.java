package com.sdg.graph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

/**
 * Class to interact with a Neo4j database.
 * 
 * @author Joakim Colloz
 * @version 1.0
 */
public class Neo4jDatabaseService implements AutoCloseable {
    private final Driver driver;

    public Neo4jDatabaseService() {
        this.driver = GraphDatabase.driver(
            Neo4jConfig.DB_URI, AuthTokens.basic(Neo4jConfig.DB_USER, Neo4jConfig.DB_PASSWORD)
        );
    }

    public void queryDatabase() {
        try (var session = driver.session()) {
            driver.verifyConnectivity();

            String query = "MATCH (n)-[r]->(m) " +
                    "RETURN n, r, m, n.code AS n_code, n.type AS n_type, m.code AS m_code, m.type AS m_type";

            session.readTransaction(tx -> {
                Result result = tx.run(query);
                while (result.hasNext()) {
                    Record rec = result.next();
                    System.out.println("Node N - Code: " + rec.get("n_code").asString() +
                            ", Type: " + rec.get("n_type").asString());
                    System.out.println("Relationship: " + rec.get("r").asMap());
                    System.out.println("Node M - Code: " + rec.get("m_code").asString() +
                            ", Type: " + rec.get("m_type").asString());
                    System.out.println("---------------------------------");
                }
                return null;
            });
        }
    }

    public void deleteAllData() {
        try (var session = driver.session()) {
            String deleteQuery = "MATCH (n) DETACH DELETE n";

            session.writeTransaction(tx -> {
                tx.run(deleteQuery);
                return null;
            });

            System.out.println("All nodes and relationships have been deleted.");
        }
    }

    @Override
    public void close() {
        if (driver != null) {
            driver.close();
        }
    }

    public static void main(String... args) {
        try (Neo4jDatabaseService service = new Neo4jDatabaseService()) {
            // Uncomment these lines to perform operations
            // service.deleteAllData();
            // service.queryDatabase();
        }
    }
}
