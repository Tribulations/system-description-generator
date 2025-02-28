package com.sdg.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Session;
import com.sdg.logging.LoggerUtil;

import static org.neo4j.driver.Values.parameters;

/**
 * Handles operations related to storing and retrieving data from the Neo4j graph database.
 * 
 * Uses Neo4j's Java driver to execute Cypher queries defined in {@link CypherConstants}.
 * 
 * @author Joakim Colloz
 * @version 1.0
 * @see CypherConstants
 */
public class GraphDatabaseOperations implements AutoCloseable {
    // TODO: Replace usage of depreacted methods in this class

    private final Driver driver;

    /**
     * Creates a new instance with default connection settings.
     */
    public GraphDatabaseOperations() {
        LoggerUtil.info(getClass(), "Initializing GraphDatabaseOperations");
        this.driver = GraphDatabase.driver(
            Neo4jConfig.DB_URI, AuthTokens.basic(Neo4jConfig.DB_USER, Neo4jConfig.DB_PASSWORD)
        );
        LoggerUtil.debug(getClass(), "Neo4j driver initialized with URI: {}", Neo4jConfig.DB_URI);
    }

    /**
     * Creates a node representing a Java class in the graph database.
     * 
     * @param className the name of the class to create
     */
    public void createClassNode(String className) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                LoggerUtil.debug(getClass(), "Creating class node: {}", className);
                tx.run(CypherConstants.CREATE_CLASS, parameters("name", className));
                return null;
            });
        }
    }

    /**
     * Creates a node representing a method and connects it to its containing class.
     * 
     * @param className the name of the class containing the method
     * @param methodName the name of the method to create
     */
    public void createMethodNode(String className, String methodName) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                LoggerUtil.debug(getClass(), "Creating method node and relationship: {}.{}", className, methodName);
                tx.run(CypherConstants.CREATE_METHOD, parameters("name", methodName));
                tx.run(CypherConstants.CONNECT_METHOD_TO_CLASS,
                        parameters("className", className, "methodName", methodName));
                return null;
            });
        }
    }

    /**
     * Creates a node representing a method call and connects it to the calling method.
     * 
     * @param methodName the name of the method making the call
     * @param methodCallName the name of the method being called
     */
    public void createMethodCallNode(String methodName, String methodCallName) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                LoggerUtil.debug(getClass(), "Creating method call node and relationship: {} -> {}", methodName, methodCallName);
                tx.run(CypherConstants.CREATE_FUNCTION_CALL, parameters("name", methodCallName));
                tx.run(CypherConstants.CONNECT_CALL_TO_METHOD,
                        parameters("methodName", methodName, "name", methodCallName));
                return null;
            });
        }
    }

    /**
     * Creates a node representing a control flow statement and connects it to its containing method.
     * 
     * @param methodName the name of the method containing the control flow
     * @param type the type of control flow (e.g., "if", "for")
     * @param condition the condition or expression of the control flow
     */
    public void createControlFlowNode(String methodName, String type, String condition) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                LoggerUtil.debug(getClass(), "Creating control flow node and relationship: {} -> {}", methodName, condition);
                tx.run(CypherConstants.CREATE_CONTROL_FLOW,
                        parameters("type", type, "condition", condition));
                tx.run(CypherConstants.CONNECT_CONTROL_TO_METHOD,
                        parameters("methodName", methodName, "condition", condition));
                return null;
            });
        }
    }

    /**
     * Deletes all data from the graph database.
     * 
     * @throws RuntimeException if there is an error during deletion
     */
    public void deleteAllData() {
        try (var session = driver.session()) {
            LoggerUtil.warn(getClass(), "Deleting all data from the database");
            session.writeTransaction(tx -> {
                LoggerUtil.debug(getClass(), "Executing delete query");
                tx.run(CypherConstants.DELETE_ALL);
                LoggerUtil.info(getClass(), "All data deleted successfully");
                return null;
            });
        } catch (Exception e) {
            LoggerUtil.error(getClass(), "Error deleting database data", e);
            throw e;
        }
    }

    /**
     * Closes the database connection.
     */
    @Override
    public void close() {
        LoggerUtil.info(getClass(), "Closing Neo4j database connection");
        if (driver != null) {
            driver.close();
        }
    }
}
