package com.sdg.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sdg.graph.model.TestModel;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Driver;

import java.io.IOException;
import java.util.Map;

import com.sdg.graph.model.ClassNode;
import com.sdg.graph.model.MethodNode;
import com.sdg.graph.model.SystemStructure;
import com.sdg.graph.model.ClassFieldNode;
import com.sdg.graph.model.ControlFlowNode;
import com.sdg.graph.model.MethodCallNode;

/**
 * Converts knowledge graph data to JSON format.
 * Uses Jackson for conversion to JSON and Neo4j Driver for database access.
 *
 * TODO: extract queries and other responsibilities. to separate class e.g. GraphDatabaseOperations
 * 
 * @see com.sdg.graph.GraphDatabaseOperations
 * @see ClassNode
 * @see MethodNode
 * @see SystemStructure
 * @see ClassFieldNode
 * @see ControlFlowNode
 * @see MethodCallNode
 * @author Joakim Colloz
 * @version 1.0
 */
public class GraphDataToJsonConverter {
    private final Driver neo4jDriver;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new GraphDataToJsonConverter with the given Neo4j driver.
     * 
     * @param neo4jDriver the Neo4j driver for database access
     */
    public GraphDataToJsonConverter(Driver neo4jDriver) {
        this.neo4jDriver = neo4jDriver;
        this.objectMapper = new ObjectMapper();

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public String toJson(TestModel testModel) {
        try {
            return objectMapper.writeValueAsString(testModel);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Extracts the most significant classes from the knowledge graph and converts them to JSON.
     * Most significant classes are defined as classes with the most relationships (methods, fields, etc.)
     *
     * TODO: This logic likely have to change. Classes with the most relationships
     * (especially where the relation is to its own class fields as this might not be the most significant).
     *
     * @param limit the maximum number of classes to include
     * @return JSON string representation of the most important classes
     * @throws IOException if conversion to JSON fails
     */
    public String jsonifyMostSignificantClasses(int limit) throws IOException {
        SystemStructure system = new SystemStructure();
        
        try (Session session = neo4jDriver.session()) {
            // Find classes with the most relationships (methods, fields, etc.) ()
            String query = 
            "MATCH (c:Class) " +
            "MATCH (c)-[r]-() " +
            "WITH c, COUNT(r) AS connections " +
            "ORDER BY connections DESC " +
            "LIMIT $limit " +
            "RETURN c.name as className";
            
            Result result = session.run(query, Map.of("limit", limit));

            while (result.hasNext()) {
                Record record = result.next();
                String className = record.get("className").asString();
                ClassNode classNode = buildClassNode(className, session);
                system.addClass(classNode);
            }
        }
        
        return objectMapper.writeValueAsString(system);
    }

    /**
     * Builds a ClassNode with all its methods, member fields, and relationships
     *
     * @param className the name of the class to build
     * @param session the Neo4j session
     * @return a populated ClassNode
     */
    private ClassNode buildClassNode(String className, Session session) {
        ClassNode classNode = new ClassNode();
        classNode.setName(className);

        // Get inheritance
        String inheritanceQuery =
                "MATCH (c:Class {name: $className})-[:EXTENDS]->(p:Class) " +
                        "RETURN p.name as parentName";

        Result inheritanceResult = session.run(inheritanceQuery, Map.of("className", className));
        while (inheritanceResult.hasNext()) {
            String parentName = inheritanceResult.next().get("parentName").asString();
            classNode.getExtendedClasses().add(parentName);
        }

        // Get implemented interfaces
        String interfacesQuery =
                "MATCH (c:Class {name: $className})-[:IMPLEMENTS]->(i:Interface) " +
                        "RETURN i.name as interfaceName";

        Result interfacesResult = session.run(interfacesQuery, Map.of("className", className));
        while (interfacesResult.hasNext()) {
            String interfaceName = interfacesResult.next().get("interfaceName").asString();
            classNode.getImplementedInterfaces().add(interfaceName);
        }
        
        // Get methods
        String methodsQuery = 
            "MATCH (c:Class {name: $className})-[:HAS_METHOD]->(m:Method) " +
            "RETURN m.name as methodName";
        
        Result methodsResult = session.run(methodsQuery, Map.of("className", className));
        while (methodsResult.hasNext()) {
            String methodName = methodsResult.next().get("methodName").asString();
            MethodNode methodNode = buildMethodNode(methodName, session);
            classNode.getMethods().add(methodNode);
        }

        // Get class fields and their names, types, and access modifiers for the specified class
        String fieldsQuery =
                "MATCH (c:Class {name: $className})-[:HAS_FIELD]->(f:ClassField) " +
                        "RETURN f.name as fieldName, f.type as fieldType, f.visibility as visibility";

        Result fieldsResult = session.run(fieldsQuery, Map.of("className", className));
        while (fieldsResult.hasNext()) {
            Record record = fieldsResult.next();
            ClassFieldNode fieldNode = new ClassFieldNode();
            fieldNode.setName(record.get("fieldName").asString());
            fieldNode.setType(record.get("fieldType").asString());
            fieldNode.setVisibility(record.get("visibility").asString());
            classNode.getFields().add(fieldNode);
        }

        return classNode;
    }

    /**
     * Builds a MethodNode with its calls and control flow
     *
     * A MethodNode represents a method with its own method calls and control flow.
     *
     * @param methodName the name of the method to build
     * @param session the Neo4j session
     * @return a fully populated MethodNode
     */
    private MethodNode buildMethodNode(String methodName, Session session) {
        MethodNode methodNode = new MethodNode();
        methodNode.setName(methodName);
        
        // Get method calls
        String callsQuery = 
            "MATCH (m:Method {name: $methodName})-[:CALLS]->(f:MethodCall) " +
            "RETURN f.name as calledMethod";
        
        Result callsResult = session.run(callsQuery, Map.of("methodName", methodName));
        while (callsResult.hasNext()) {
            String calledMethod = callsResult.next().get("calledMethod").asString();
            methodNode.getMethodCalls().add(new MethodCallNode(calledMethod));
        }

        // Get control flow
        String controlFlowQuery =
                "MATCH (m:Method {name: $methodName})-[:CONTAINS]->(c:ControlFlow) " +
                        "RETURN c.type as type, c.condition as condition";

        Result controlFlowResult = session.run(controlFlowQuery, Map.of("methodName", methodName));
        while (controlFlowResult.hasNext()) {
            Record record = controlFlowResult.next();
            ControlFlowNode controlFlowNode = new ControlFlowNode();
            controlFlowNode.setType(record.get("type").asString());
            controlFlowNode.setCondition(record.get("condition").asString());
            methodNode.getControlFlow().add(controlFlowNode);
        }

        return methodNode;
    }


    public static void main(String... args) throws IOException {
        System.out.println(getTopLevelNodesAsJSONString());
    }

    public static String getTopLevelNodesAsJSONString() throws IOException {
        try (GraphDatabaseOperations dbOps = new GraphDatabaseOperations()) {
            GraphDataToJsonConverter graphDataToJsonConverter = new GraphDataToJsonConverter(dbOps.getDriver());

            // Get the most significant classes as JSON
            String json = graphDataToJsonConverter.jsonifyMostSignificantClasses(3);
    
            return json;
        }
    }
}