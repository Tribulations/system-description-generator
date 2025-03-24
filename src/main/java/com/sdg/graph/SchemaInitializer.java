package com.sdg.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import com.sdg.logging.LoggerUtil;

/**
 * This class handles the initialization of Neo4j schema with appropriate constraints and indexes.
 * This class is used by the {@link KnowledgeGraphService}.
 *
 * @author Joakim Colloz
 * @version 1.0
 */
public class SchemaInitializer {
    private final Driver driver;

    /**
     * Creates a new SchemaInitializer with the provided Neo4j driver.
     *
     * @param driver the Neo4j driver to use for schema initialization
     */
    public SchemaInitializer(Driver driver) {
        this.driver = driver;
    }

    /**
     * Initializes the Neo4j schema by creating necessary constraints and indexes.
     * Indexes are creates automatically when constraints are created,
     * therefore none are created for classes and interfaces.
     */
    public void initializeSchema() {
        LoggerUtil.info(getClass(), "Initializing Neo4j schema");

        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(),
                        "Creating uniqueness constraints for primary entities to improve MERGE performance");
                tx.run(CypherConstants.CREATE_CLASS_CONSTRAINTS);
                tx.run(CypherConstants.CREATE_INTERFACE_CONSTRAINTS);

                LoggerUtil.debug(getClass(), "Creating indexes for performance");
                tx.run(CypherConstants.CREATE_IMPORT_INDEXES);
                tx.run(CypherConstants.CREATE_METHOD_INDEXES);
                tx.run(CypherConstants.CREATE_METHOD_CALL_INDEXES);
                tx.run(CypherConstants.CREATE_CLASS_FIELD_INDEXES);
                tx.run(CypherConstants.CREATE_CONTROL_FLOW_INDEXES);

                return null;
            });

            LoggerUtil.info(getClass(), "Neo4j schema initialization completed successfully");
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "Failed to initialize Neo4j schema: {}", e.getMessage(), e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }
}
