package com.sdg.graph;

/**
 * Constants class containing all Cypher queries used in the application.
 * Each query is documented with its purpose and expected parameters.
 * All node creation queries use MERGE to ensure duplicates are not created.
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

    /** Query to create a MethodCall node. Parameters: name */
    public static final String CREATE_METHOD_CALL =
        "MERGE (f:MethodCall {name: $name})";

    /** Query to connect MethodCall to Method. Parameters: methodName, name */
    public static final String CONNECT_CALL_TO_METHOD = 
        "MATCH (m:Method {name: $methodName}), (f:MethodCall {name: $name}) " +
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

    /** Query to connect Class to its parent class. Parameters: className, parentName */
    public static final String CONNECT_CLASS_INHERITANCE = 
        "MATCH (c:Class {name: $className}), (p:Class {name: $parentName}) " +
        "MERGE (c)-[:EXTENDS]->(p)";

    /** Query to connect Class to implemented interface. Parameters: className, interfaceName */
    public static final String CONNECT_INTERFACE_IMPLEMENTATION = 
        "MATCH (c:Class {name: $className}), (i:Interface {name: $interfaceName}) " +
        "MERGE (c)-[:IMPLEMENTS]->(i)";

    /** Query to create Interface node. Parameters: name */
    public static final String CREATE_INTERFACE = 
        "MERGE (i:Interface {name: $name})";

    /** Query to create ClassField node. Parameters: name, type, visibility */
    public static final String CREATE_CLASS_FIELD = 
        "MERGE (f:ClassField {name: $name, type: $type, visibility: $visibility})";

    /** Query to connect ClassField to its Class. Parameters: className, fieldName */
    public static final String CONNECT_FIELD_TO_CLASS = 
        "MATCH (c:Class {name: $className}), (f:ClassField {name: $fieldName}) " +
        "MERGE (c)-[:HAS_FIELD]->(f)";
}
