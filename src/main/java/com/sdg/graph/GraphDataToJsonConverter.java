package com.sdg.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

    // Node property names
    private static final String PROP_CLASS_NAME = "className";
    private static final String PROP_METHOD_NAME = "methodName";
    private static final String PROP_FIELD_NAME = "fieldName";
    private static final String PROP_FIELD_TYPE = "fieldType";
    private static final String PROP_VISIBILITY = "visibility";
    private static final String PROP_INTERFACE_NAME = "interfaceName";
    private static final String PROP_PARENT_NAME = "parentName";
    private static final String PROP_CALLED_METHOD = "calledMethod";
    private static final String PROP_TYPE = "type";
    private static final String PROP_CONDITION = "condition";
    private static final String PROP_LIMIT = "limit";

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
            Result result = session.run(CypherConstants.FIND_CLASSES_WITH_MOST_RELATIONSHIPS, Map.of(PROP_LIMIT, limit));

            while (result.hasNext()) {
                Record record = result.next();
                String className = record.get(PROP_CLASS_NAME).asString();
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

        getInheritance(className, session, classNode);
        getImplementedInterfaces(className, session, classNode);
        getMethods(className, session, classNode);
        getMemberFields(className, session, classNode);

        return classNode;
    }

    /**
     * Get class fields and their names, types, and access modifiers for the specified class.
     * 
     * @param className the name of the class
     * @param session the Neo4j session
     * @param classNode the ClassNode to populate
     */
    private void getMemberFields(String className, Session session, ClassNode classNode) {
        Result fieldsResult = session.run(CypherConstants.GET_CLASS_FIELDS, Map.of(PROP_CLASS_NAME, className));
        while (fieldsResult.hasNext()) {
            Record record = fieldsResult.next();
            ClassFieldNode fieldNode = new ClassFieldNode();
            fieldNode.setName(record.get(PROP_FIELD_NAME).asString());
            fieldNode.setType(record.get(PROP_FIELD_TYPE).asString());
            fieldNode.setVisibility(record.get(PROP_VISIBILITY).asString());
            classNode.getFields().add(fieldNode);
        }
    }

    private void getMethods(String className, Session session, ClassNode classNode) {
        Result methodsResult = session.run(CypherConstants.GET_CLASS_METHODS, Map.of(PROP_CLASS_NAME, className));
        while (methodsResult.hasNext()) {
            String methodName = methodsResult.next().get(PROP_METHOD_NAME).asString();
            MethodNode methodNode = buildMethodNode(methodName, session);
            classNode.getMethods().add(methodNode);
        }
    }

    private void getImplementedInterfaces(String className, Session session, ClassNode classNode) {
        Result interfacesResult = session.run(CypherConstants.GET_CLASS_INTERFACES, Map.of(PROP_CLASS_NAME, className));
        while (interfacesResult.hasNext()) {
            String interfaceName = interfacesResult.next().get(PROP_INTERFACE_NAME).asString();
            classNode.getImplementedInterfaces().add(interfaceName);
        }
    }

    private void getInheritance(String className, Session session, ClassNode classNode) {
        Result inheritanceResult = session.run(CypherConstants.GET_CLASS_INHERITANCE, Map.of(PROP_CLASS_NAME, className));
        while (inheritanceResult.hasNext()) {
            String parentName = inheritanceResult.next().get(PROP_PARENT_NAME).asString();
            classNode.getExtendedClasses().add(parentName);
        }
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

        getMethodCalls(methodName, session, methodNode);
        getControlFlow(methodName, session, methodNode);

        return methodNode;
    }

    private void getControlFlow(String methodName, Session session, MethodNode methodNode) {
        Result controlFlowResult = session.run(CypherConstants.GET_CONTROL_FLOW, Map.of(PROP_METHOD_NAME, methodName));
        while (controlFlowResult.hasNext()) {
            Record record = controlFlowResult.next();
            ControlFlowNode controlFlowNode = new ControlFlowNode();
            controlFlowNode.setType(record.get(PROP_TYPE).asString());
            controlFlowNode.setCondition(record.get(PROP_CONDITION).asString());
            methodNode.getControlFlow().add(controlFlowNode);
        }
    }

    private void getMethodCalls(String methodName, Session session, MethodNode methodNode) {
        Result callsResult = session.run(CypherConstants.GET_METHOD_CALLS, Map.of(PROP_METHOD_NAME, methodName));
        while (callsResult.hasNext()) {
            String calledMethod = callsResult.next().get(PROP_CALLED_METHOD).asString();
            methodNode.getMethodCalls().add(new MethodCallNode(calledMethod));
        }
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
