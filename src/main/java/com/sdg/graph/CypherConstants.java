package com.sdg.graph;

/**
 * Constants class containing all Cypher queries used in the application.
 * Each query is documented with its purpose and expected parameters.
 */
public final class CypherConstants {
    private CypherConstants() {
        // Prevent instantiation
    }

    /** Query to create a new Class node. Parameters: name */
    public static final String CREATE_CLASS = 
        "MERGE (c:Class {name: $name})";

    /** Query to create a new Method node. Parameters: name */
    public static final String CREATE_METHOD = 
        "MERGE (m:Method {name: $name})";

    /** Query to connect Method to its Class. Parameters: className, methodName */
    public static final String CONNECT_METHOD_TO_CLASS = 
        "MATCH (c:Class {name: $className}), (m:Method {name: $methodName}) " +
        "MERGE (c)-[:HAS_METHOD]->(m)";

    /** Query to create a FunctionCall node. Parameters: name */
    public static final String CREATE_FUNCTION_CALL = 
        "MERGE (f:FunctionCall {name: $name})";

    /** Query to connect FunctionCall to Method. Parameters: methodName, name */
    public static final String CONNECT_CALL_TO_METHOD = 
        "MATCH (m:Method {name: $methodName}), (f:FunctionCall {name: $name}) " +
        "MERGE (m)-[:CALLS]->(f)";

    /** Query to create a ControlFlow node. Parameters: type, condition */
    public static final String CREATE_CONTROL_FLOW = 
        "MERGE (ctrl:ControlFlow {type: $type, condition: $condition})";

    /** Query to connect ControlFlow to Method. Parameters: methodName, condition */
    public static final String CONNECT_CONTROL_TO_METHOD = 
        "MATCH (m:Method {name: $methodName}), (ctrl:ControlFlow {condition: $condition}) " +
        "MERGE (m)-[:CONTAINS]->(ctrl)";

    /** Query to get all relationships in the graph */
    public static final String GET_ALL_RELATIONSHIPS = 
        "MATCH (n)-[r]->(m) " +
        "RETURN n, r, m, n.code AS n_code, n.type AS n_type, m.code AS m_code, m.type AS m_type";

    /** Query to delete all nodes and relationships */
    public static final String DELETE_ALL = 
        "MATCH (n) DETACH DELETE n";
}
