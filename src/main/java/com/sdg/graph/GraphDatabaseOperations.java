//package com.sdg.graph;
//
//import org.neo4j.driver.Driver;
//import org.neo4j.driver.GraphDatabase;
//import org.neo4j.driver.AuthTokens;
//import org.neo4j.driver.Session;
//
//import org.slf4j.Logger;
//import com.sdg.logging.LoggerUtil;
//
//import static org.neo4j.driver.Values.parameters;
//
//public class GraphDatabaseOperations implements AutoCloseable {
//    // TODO: Replace usage of this class with depreacted methods in this class
//
//    private static final Logger logger = LoggerUtil.getLogger(GraphDatabaseOperations.class);
//    private final Driver driver;
//
//    /**
//     * Creates a new instance with default connection settings.
//     */
//    public GraphDatabaseOperations() {
//        logger.info("Initializing GraphDatabaseOperations");
//        this.driver = GraphDatabase.driver(
//            Neo4jConfig.DB_URI, AuthTokens.basic(Neo4jConfig.DB_USER, Neo4jConfig.DB_PASSWORD)
//        );
//        logger.debug("Neo4j driver initialized with URI: {}", Neo4jConfig.DB_URI);
//    }
//
//    public void createClassNode(String className) {
//        try (Session session = driver.session()) {
//            session.writeTransaction(tx -> {
//                logger.debug("Creating class node: {}", className);
//                tx.run(CypherConstants.CREATE_CLASS, parameters("name", className));
//                return null;
//            });
//        }
//    }
//
//    public void createMethodNode(String className, String methodName) {
//        try (Session session = driver.session()) {
//            session.writeTransaction(tx -> {
//                logger.debug("Creating method node and relationship: {}.{}", className, methodName);
//                tx.run(CypherConstants.CREATE_METHOD, parameters("name", methodName));
//                tx.run(CypherConstants.CONNECT_METHOD_TO_CLASS,
//                        parameters("className", className, "methodName", methodName));
//                return null;
//            });
//        }
//    }
//
//    public void createMethodCallNode(String methodName, String methodCallName) {
//        try (Session session = driver.session()) {
//            session.writeTransaction(tx -> {
//                logger.debug("Creating method call node and relationship: {} -> {}", methodName, methodCallName);
//                tx.run(CypherConstants.CREATE_FUNCTION_CALL, parameters("name", methodCallName));
//                tx.run(CypherConstants.CONNECT_CALL_TO_METHOD,
//                        parameters("methodName", methodName, "name", methodCallName));
//                return null;
//            });
//        }
//    }
//
//    public void createControlFlowNode(String methodName, String type, String condition) {
//        try (Session session = driver.session()) {
//            session.writeTransaction(tx -> {
//                logger.debug("Creating control flow node and relationship: {} -> {}", methodName, condition);
//                tx.run(CypherConstants.CREATE_CONTROL_FLOW,
//                        parameters("type", type, "condition", condition));
//                tx.run(CypherConstants.CONNECT_CONTROL_TO_METHOD,
//                        parameters("methodName", methodName, "condition", condition));
//                return null;
//            });
//        }
//    }
//
//    public void deleteAllData() {
//        try (var session = driver.session()) {
//            logger.warn("Deleting all data from the database");
//            session.writeTransaction(tx -> {
//                logger.debug("Executing delete query");
//                tx.run(CypherConstants.DELETE_ALL);
//                logger.info("All data deleted successfully");
//                return null;
//            });
//        } catch (Exception e) {
//            logger.error("Error deleting database data", e);
//            throw e;
//        }
//    }
//
//    @Override
//    public void close() {
//        logger.info("Closing Neo4j database connection");
//        if (driver != null) {
//            driver.close();
//        }
//    }
//}
