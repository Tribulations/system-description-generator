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
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating class node: {}", className);
                tx.run(CypherConstants.CREATE_CLASS, parameters(CypherConstants.PROP_CLASS_NAME, 
                className));
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
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating method node and relationship: {}.{}", className, methodName);
                tx.run(CypherConstants.CREATE_METHOD, parameters(CypherConstants.PROP_METHOD_NAME, methodName));
                tx.run(CypherConstants.CONNECT_METHOD_TO_CLASS,
                        parameters(CypherConstants.PROP_CLASS_NAME, className, 
                                 CypherConstants.PROP_METHOD_NAME, methodName));
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
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating method call node and relationship: {} -> {}", methodName, methodCallName);
                tx.run(CypherConstants.CREATE_METHOD_CALL, parameters(CypherConstants.PROP_CALLED_METHOD, methodCallName));
                tx.run(CypherConstants.CONNECT_CALL_TO_METHOD,
                        parameters(CypherConstants.PROP_METHOD_NAME, methodName, 
                                 CypherConstants.PROP_CALLED_METHOD, methodCallName));
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
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating control flow node and relationship: {} -> {}", methodName, condition);
                tx.run(CypherConstants.CREATE_CONTROL_FLOW,
                        parameters(CypherConstants.PROP_TYPE, type, 
                                 CypherConstants.PROP_CONDITION, condition));
                tx.run(CypherConstants.CONNECT_CONTROL_TO_METHOD,
                        parameters(CypherConstants.PROP_METHOD_NAME, methodName, 
                                 CypherConstants.PROP_CONDITION, condition));
                return null;
            });
        }
    }

    /**
     * Creates a relationship representing class inheritance.
     * 
     * @param className the name of the child class
     * @param parentName the name of the parent class
     */
    public void createInheritanceRelationship(String className, String parentName) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating inheritance relationship: {} extends {}", className, parentName);
                tx.run(CypherConstants.CONNECT_CLASS_INHERITANCE,
                        parameters(CypherConstants.PROP_CLASS_NAME, className, 
                                 CypherConstants.PROP_PARENT_NAME, parentName));
                return null;
            });
        }
    }

    /**
     * Creates an interface node and connects it to the implementing class.
     * 
     * @param className the name of the implementing class
     * @param interfaceName the name of the interface being implemented
     */
    public void createInterfaceImplementation(String className, String interfaceName) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating interface implementation: {} implements {}", className, interfaceName);
                tx.run(CypherConstants.CREATE_INTERFACE, parameters(CypherConstants.PROP_INTERFACE_NAME, interfaceName));
                tx.run(CypherConstants.CONNECT_INTERFACE_IMPLEMENTATION,
                        parameters(CypherConstants.PROP_CLASS_NAME, className, 
                                 CypherConstants.PROP_INTERFACE_NAME, interfaceName));
                return null;
            });
        }
    }

    /**
     * Creates a node representing a class field and connects it to its containing class.
     * 
     * @param className the name of the class containing the field
     * @param fieldName the name of the field
     * @param fieldType the type of the field
     * @param visibility the access modifier of the field (public, private, protected or package-private)
     */
    public void createClassField(String className, String fieldName, String fieldType, String visibility) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating class field: {}.{} ({} {})", className, fieldName, visibility, fieldType);
                tx.run(CypherConstants.CREATE_CLASS_FIELD,
                        parameters(CypherConstants.PROP_FIELD_NAME, fieldName, 
                                 CypherConstants.PROP_FIELD_TYPE, fieldType, 
                                 CypherConstants.PROP_VISIBILITY, visibility));
                tx.run(CypherConstants.CONNECT_FIELD_TO_CLASS,
                        parameters(CypherConstants.PROP_CLASS_NAME, className, 
                                 CypherConstants.PROP_FIELD_NAME, fieldName));
                return null;
            });
        }
    }

    /**
     * Creates a node representing an import statement and connects it to the importing class.
     * 
     * @param className the name of the class containing the import
     * @param importName the fully qualified name of the imported type
     */
    public void createImportRelationship(String className, String importName) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                LoggerUtil.debug(getClass(), "Creating import relationship: {} imports {}", className, importName);
                tx.run(CypherConstants.CREATE_IMPORT, parameters(CypherConstants.PROP_IMPORT_NAME, importName));
                tx.run(CypherConstants.CONNECT_CLASS_IMPORT,
                        parameters(CypherConstants.PROP_CLASS_NAME, className, 
                                 CypherConstants.PROP_IMPORT_NAME, importName));
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
            session.executeWrite(tx -> {
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
     * Returns the Neo4j driver instance.
     *
     * @return the Neo4j driver instance
     */
    public Driver getDriver() {
        return driver;
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
