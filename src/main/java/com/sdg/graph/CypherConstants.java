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

    // Node property names
    public static final String PROP_CLASS_NAME = "className";
    public static final String PROP_METHOD_NAME = "methodName";
    public static final String PROP_FIELD_NAME = "fieldName";
    public static final String PROP_FIELD_TYPE = "fieldType";
    public static final String PROP_VISIBILITY = "visibility";
    public static final String PROP_INTERFACE_NAME = "interfaceName";
    public static final String PROP_PARENT_NAME = "parentName";
    public static final String PROP_CALLED_METHOD = "calledMethod";
    public static final String PROP_TYPE = "type";
    public static final String PROP_CONDITION = "condition";
    public static final String PROP_LIMIT = "limit";

    /** Query to create a new Class node. Parameters: name */
    public static final String CREATE_CLASS = 
        "MERGE (c:Class {className: $className})";

    /** Query to create a new Method node. Parameters: name */
    public static final String CREATE_METHOD = 
        "MERGE (m:Method {methodName: $methodName})";

    /** Query to connect Method to its Class. Parameters: className, methodName */
    public static final String CONNECT_METHOD_TO_CLASS = 
        "MATCH (c:Class {className: $className}), (m:Method {methodName: $methodName}) " +
        "MERGE (c)-[:HAS_METHOD]->(m)";

    /** Query to create a MethodCall node. Parameters: name */
    public static final String CREATE_METHOD_CALL =
        "MERGE (f:MethodCall {calledMethod: $calledMethod})";

    /** Query to connect MethodCall to Method. Parameters: methodName, name */
    public static final String CONNECT_CALL_TO_METHOD = 
        "MATCH (m:Method {methodName: $methodName}), (f:MethodCall {calledMethod: $calledMethod}) " +
        "MERGE (m)-[:CALLS]->(f)";

    /** Query to create a ControlFlow node. Parameters: type, condition */
    public static final String CREATE_CONTROL_FLOW = 
        "MERGE (ctrl:ControlFlow {type: $type, condition: $condition})";

    /** Query to connect ControlFlow to Method. Parameters: methodName, condition */
    public static final String CONNECT_CONTROL_TO_METHOD = 
        "MATCH (m:Method {methodName: $methodName}), (ctrl:ControlFlow {condition: $condition}) " +
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
        "MATCH (c:Class {className: $className}), (p:Class {className: $parentName}) " +
        "MERGE (c)-[:EXTENDS]->(p)";

    /** Query to connect Class to implemented interface. Parameters: className, interfaceName */
    public static final String CONNECT_INTERFACE_IMPLEMENTATION = 
        "MATCH (c:Class {className: $className}), (i:Interface {className: $interfaceName}) " +
        "MERGE (c)-[:IMPLEMENTS]->(i)";

    /** Query to create Interface node. Parameters: name */
    public static final String CREATE_INTERFACE = 
        "MERGE (i:Interface {interfaceName: $interfaceName})";

    /** Query to create ClassField node. Parameters: name, type, visibility */
    public static final String CREATE_CLASS_FIELD = 
        "MERGE (f:ClassField {fieldName: $fieldName, fieldType: $fieldType, visibility: $visibility})";

    /** Query to connect ClassField to its Class. Parameters: className, fieldName */
    public static final String CONNECT_FIELD_TO_CLASS = 
        "MATCH (c:Class {className: $className}), (f:ClassField {fieldName: $fieldName}) " +
        "MERGE (c)-[:HAS_FIELD]->(f)";

    /** Query to find classes with the most relationships (methods, fields, etc.) Parameters: limit */
    public static final String FIND_CLASSES_WITH_MOST_RELATIONSHIPS = 
        "MATCH (c:Class) " +
        "MATCH (c)-[r]-() " +
        "WITH c, COUNT(r) AS connections " +
        "ORDER BY connections DESC " +
        "LIMIT $limit " +
        "RETURN c.className as className";

    /** Query to get control flow of a method. Parameters: methodName */
    public static final String GET_CONTROL_FLOW =
        "MATCH (m:Method {methodName: $methodName})-[:CONTAINS]->(c:ControlFlow) " +
                "RETURN c.type as type, c.condition as condition";

    /** Query to get fields of a class. Parameters: className */
    public static final String GET_CLASS_FIELDS =
        "MATCH (c:Class {className: $className})-[:HAS_FIELD]->(f:ClassField) " +
                "RETURN f.fieldName as fieldName, f.fieldType as fieldType, f.visibility as visibility";

    /** Query to get methods of a class. Parameters: className */
    public static final String GET_CLASS_METHODS =
        "MATCH (c:Class {className: $className})-[:HAS_METHOD]->(m:Method) " +
                "RETURN m.methodName as methodName";

    /** Query to get implemented interfaces of a class. Parameters: className */
    public static final String GET_CLASS_INTERFACES =
        "MATCH (c:Class {className: $className})-[:IMPLEMENTS]->(i:Interface) " +
                "RETURN i.interfaceName as interfaceName";

    /** Query to get parent class of a class. Parameters: className */
    public static final String GET_CLASS_INHERITANCE =
        "MATCH (c:Class {className: $className})-[:EXTENDS]->(p:Class) " +
                "RETURN p.className as parentName";

    /** Query to get method calls of a method. Parameters: methodName */
    public static final String GET_METHOD_CALLS =
        "MATCH (m:Method {methodName: $methodName})-[:CALLS]->(f:MethodCall) " +
                "RETURN f.calledMethod as calledMethod";
}
