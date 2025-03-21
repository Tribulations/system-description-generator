package com.sdg.graph;

import com.sdg.logging.LoggerUtil;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import static org.neo4j.driver.Values.parameters;

/**
 * Handles operations related to storing and retrieving data from the Neo4j graph database.
 *
 * Uses Neo4j's Java driver to execute Cypher queries defined in {@link CypherConstants}.
 *
 * // TODO add doc about batch processing and methods to call
 * @author Joakim Colloz
 * @version 1.0
 * @see CypherConstants
 */
public class GraphDatabaseOperations implements AutoCloseable {
    private final Driver driver;
    private Session batchSession;
    private Transaction batchTransaction;

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
     * Returns the Neo4j driver instance.
     *
     * @return the Neo4j driver instance
     */
    public Driver getDriver() {
        return driver;
    }

    /**
     * Starts a batch session for more efficient database operations when processing multiple files.
     * This should be called before processing a batch of operations.
     */
    public void startBatchSession() {
        if (batchSession != null) {
            LoggerUtil.warn(getClass(), "Batch session already started, closing existing session first");
            endBatchSession();
        }

        LoggerUtil.info(getClass(), "Starting batch session");
        batchSession = driver.session();
    }

    /**
     * Starts a batch transaction within the current batch session.
     * This should be called before processing a file to group all its operations in a single transaction.
     *
     * @throws IllegalStateException if no batch session has been started
     */
    public void startBatchTransaction() {
        if (batchSession == null) {
            throw new IllegalStateException("Cannot start batch transaction: No batch session has been started");
        }

        if (batchTransaction != null) {
            LoggerUtil.warn(getClass(), "Batch transaction already started, committing existing transaction first");
            commitBatchTransaction();
        }

        LoggerUtil.debug(getClass(), "Starting batch transaction");
        batchTransaction = batchSession.beginTransaction();
    }

    /**
     * Commits the current batch transaction.
     * This should be called after all operations for a file have been executed.
     *
     * @throws IllegalStateException if no batch transaction has been started
     */
    public void commitBatchTransaction() {
        if (batchTransaction == null) {
            throw new IllegalStateException("Cannot commit batch transaction: No batch transaction has been started");
        }

        LoggerUtil.debug(getClass(), "Committing batch transaction");
        batchTransaction.commit();
        batchTransaction.close();
        batchTransaction = null;
    }

    /**
     * Rolls back the current batch transaction in case of errors.
     *
     * @throws IllegalStateException if no batch transaction has been started
     */
    public void rollbackBatchTransaction() {
        if (batchTransaction == null) {
            throw new IllegalStateException("No batch transaction has been started");
        }
        batchTransaction.rollback();
        batchTransaction.close();
        batchTransaction = null;
    }

    /**
     * Ends the current batch session and releases all resources.
     * This should be called after all files have been processed.
     */
    public void endBatchSession() {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Closing open batch transaction");
            batchTransaction.close();
            batchTransaction = null;
        }

        if (batchSession != null) {
            LoggerUtil.info(getClass(), "Ending batch session");
            batchSession.close();
            batchSession = null;
        }
    }

    /**
     * Checks if a batch transaction is currently active.
     *
     * @return true if a batch transaction is active, false otherwise
     */
    public boolean isBatchTransactionActive() {
        return batchTransaction != null;
    }

    /**
     * Checks if a batch session is currently active.
     *
     * @return true if a batch session is active, false otherwise
     */
    public boolean isBatchSessionActive() {
        return batchSession != null;
    }

    /**
     * Creates a node representing a Java class in the graph database.
     *
     * @param className the name of the class to create
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createClassNode(String className) throws IllegalStateException {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating class node in batch transaction: {}", className);
            batchTransaction.run(CypherConstants.CREATE_CLASS, parameters(CypherConstants.PROP_CLASS_NAME, className));
        } else {
            throw new IllegalStateException("Cannot create class node outside of a batch transaction");
        }
    }

    /**
     * Creates a node representing a method and connects it to its containing class.
     *
     * @param className the name of the class containing the method
     * @param methodName the name of the method to create
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createMethodNode(String className, String methodName) throws IllegalStateException {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating method node in batch transaction: {}.{}", className, methodName);
            batchTransaction.run(CypherConstants.CREATE_METHOD,
                    parameters(CypherConstants.PROP_METHOD_NAME, methodName));
            batchTransaction.run(CypherConstants.CONNECT_METHOD_TO_CLASS,
                    parameters(CypherConstants.PROP_CLASS_NAME, className,
                            CypherConstants.PROP_METHOD_NAME, methodName));
        } else {
            throw new IllegalStateException("Cannot create method node outside of a batch transaction");
        }
    }

    /**
     * Creates a node representing a method call between two methods.
     *
     * @param callerMethod the name of the method making the call
     * @param calledMethod the name of the method being called
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createMethodCallNode(String callerMethod, String calledMethod) throws IllegalStateException {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating method call in batch transaction: {} -> {}", callerMethod, calledMethod);
            batchTransaction.run(CypherConstants.CREATE_METHOD_CALL,
                    parameters(CypherConstants.PROP_CALLED_METHOD, calledMethod));
            batchTransaction.run(CypherConstants.CONNECT_CALL_TO_METHOD,
                    parameters(CypherConstants.PROP_METHOD_NAME, callerMethod,
                            CypherConstants.PROP_CALLED_METHOD, calledMethod));
        } else {
            throw new IllegalStateException("Cannot create method call node outside of a batch transaction");
        }
    }

    /**
     * Creates a relationship representing inheritance between two classes.
     *
     * @param childClass the name of the child class
     * @param parentClass the name of the parent class
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createInheritanceRelationship(String childClass, String parentClass) throws IllegalStateException {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating inheritance relationship in batch transaction: {} extends {}", childClass, parentClass);
            batchTransaction.run(CypherConstants.CONNECT_CLASS_INHERITANCE,
                    parameters(CypherConstants.PROP_CLASS_NAME, childClass,
                            CypherConstants.PROP_PARENT_NAME, parentClass));
        } else {
            throw new IllegalStateException("Cannot create class node outside of a batch transaction");
        }
    }

    /**
     * Creates a relationship representing an interface implementation.
     *
     * @param implementingClass the name of the class implementing the interface
     * @param interfaceName the name of the interface being implemented
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createInterfaceImplementation(String implementingClass, String interfaceName) {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating interface implementation in batch transaction: {} implements {}", implementingClass, interfaceName);
            batchTransaction.run(CypherConstants.CREATE_INTERFACE,
                    parameters(CypherConstants.PROP_INTERFACE_NAME, interfaceName));
            batchTransaction.run(CypherConstants.CONNECT_INTERFACE_IMPLEMENTATION,
                    parameters(CypherConstants.PROP_CLASS_NAME, implementingClass,
                            CypherConstants.PROP_INTERFACE_NAME, interfaceName));
        } else {
            throw new IllegalStateException("Cannot create interface implementation outside of a batch transaction");
        }
    }

    /**
     * Creates a relationship representing an import statement in a class.
     *
     * @param className the name of the class with the import
     * @param importName the name of the imported package or class
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createImportRelationship(String className, String importName) {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating import relationship in batch transaction: {} imports {}", className, importName);
            batchTransaction.run(CypherConstants.CREATE_IMPORT,
                    parameters(CypherConstants.PROP_IMPORT_NAME, importName));
            batchTransaction.run(CypherConstants.CONNECT_CLASS_IMPORT,
                    parameters(CypherConstants.PROP_CLASS_NAME, className,
                            CypherConstants.PROP_IMPORT_NAME, importName));
        } else {
            throw new IllegalStateException("Cannot create import relationship outside of a batch transaction");
        }
    }

    /**
     * Creates a node representing a field in a class.
     *
     * @param className the name of the class containing the field
     * @param fieldName the name of the field
     * @param fieldType the type of the field
     * @param accessModifier the access modifier of the field (public, private, etc.)
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createClassField(String className, String fieldName, String fieldType, String accessModifier) {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating class field in batch transaction: {}.{} ({} {})", className, fieldName, accessModifier, fieldType);
            batchTransaction.run(CypherConstants.CREATE_CLASS_FIELD,
                    parameters(CypherConstants.PROP_FIELD_NAME, fieldName,
                            CypherConstants.PROP_FIELD_TYPE, fieldType,
                            CypherConstants.PROP_VISIBILITY, accessModifier));
            batchTransaction.run(CypherConstants.CONNECT_FIELD_TO_CLASS,
                    parameters(CypherConstants.PROP_CLASS_NAME, className,
                            CypherConstants.PROP_FIELD_NAME, fieldName));
        } else {
            throw new IllegalStateException("Cannot create class field outside of a batch transaction");
        }
    }

    /**
     * Creates a node representing a control flow statement in a method.
     *
     * @param methodName the name of the method containing the control flow
     * @param controlFlowType the type of control flow (if, for, while, etc.)
     * @param condition the condition of the control flow statement
     * @throws IllegalStateException if no batch transaction is active
     */
    public void createControlFlowNode(String methodName, String controlFlowType, String condition) {
        if (batchTransaction != null) {
            LoggerUtil.debug(getClass(), "Creating control flow node in batch transaction: {} in method {}", controlFlowType, methodName);
            batchTransaction.run(CypherConstants.CREATE_CONTROL_FLOW,
                    parameters(CypherConstants.PROP_TYPE, controlFlowType,
                            CypherConstants.PROP_CONDITION, condition));
            batchTransaction.run(CypherConstants.CONNECT_CONTROL_TO_METHOD,
                    parameters(CypherConstants.PROP_METHOD_NAME, methodName,
                            CypherConstants.PROP_CONDITION, condition));
        } else {
            throw new IllegalStateException("Cannot create control flow node outside of a batch transaction");
        }
    }

    /**
     * Deletes all data from the graph database.
     * This should be called at the start of each test, not between processing individual files.
     */
    public void deleteAllData() {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                LoggerUtil.info(getClass(), "Deleting all data from the graph database");
                tx.run(CypherConstants.DELETE_ALL);
                return null;
            });
        }
    }

    @Override
    public void close() {
        LoggerUtil.info(getClass(), "Closing GraphDatabaseOperations");
        endBatchSession();
        driver.close();
    }
}
