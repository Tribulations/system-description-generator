package com.sdg.graph;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sdg.model.TestModel;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Driver;

import java.io.IOException;
import java.util.Map;

import com.sdg.model.ClassNode;
import com.sdg.model.MethodNode;
import com.sdg.model.SystemStructure;
import com.sdg.model.ClassFieldNode;
import com.sdg.model.ControlFlowNode;
import com.sdg.model.MethodCallNode;

/**
 * Converts knowledge graph data to JSON format.
 * Uses Jackson for conversion to JSON and Neo4j Driver for database access.
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

    public String jsonifyMostSignificantClasses(int limit) throws IOException {
        SystemStructure system = new SystemStructure();
        
        try (Session session = neo4jDriver.session()) {
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

    private ClassNode buildClassNode(String className, Session session) {
        ClassNode classNode = new ClassNode();
        classNode.setName(className);
        
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

        return classNode;
    }

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