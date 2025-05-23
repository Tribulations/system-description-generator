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

    /**
     * Extracts the most significant classes from the knowledge graph and converts them to JSON.
     * Most significant classes are defined as classes with the most relationships (methods, fields, etc.)
     *
     * TODO: This logic likely have to change. Classes with the most relationships
     * (especially where the relation is to its own class fields as this might not be the most significant).
     *
     * @param limit the maximum number of classes to include
     * @param systemName the name of the system being analyzed
     * @return JSON string representation of the most important classes
     * @throws IOException if conversion to JSON fails
     */
    public String jsonifyMostSignificantClasses(int limit, String systemName) throws IOException {
        SystemStructure system = new SystemStructure(systemName);
        
        try (Session session = neo4jDriver.session()) {            
            Result result = session.run(CypherConstants.FIND_CLASSES_WITH_MOST_RELATIONSHIPS,
                    Map.of(CypherConstants.PROP_LIMIT, limit));

            while (result.hasNext()) {
                Record record = result.next();
                String className = record.get(CypherConstants.PROP_CLASS_NAME).asString();
                String packageName = record.get(CypherConstants.PROP_PACKAGE_NAME).asString("<None>");
                ClassNode classNode = buildClassNode(className, packageName, session);
                system.addClass(classNode);
            }
        }
        
        return objectMapper.writeValueAsString(system);
    }

    /**
     * Overloaded method that uses a default system name
     */
    public String jsonifyMostSignificantClasses(int limit) throws IOException {
        return jsonifyMostSignificantClasses(limit, "Not specified");
    }

    public String jsonifyAllClasses(int limit, String systemName) throws IOException {
        SystemStructure system = new SystemStructure(systemName);

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(CypherConstants.FIND_ALL_CLASSES);

            while (result.hasNext()) {
                Record record = result.next();
                String className = record.get(CypherConstants.PROP_CLASS_NAME).asString();
                String packageName = record.get(CypherConstants.PROP_PACKAGE_NAME).asString("<None>");
                ClassNode classNode = buildClassNode(className, packageName, session);
                system.addClass(classNode);
            }
        }

        return objectMapper.writeValueAsString(system);
    }

    /**
     * Overloaded method that uses a default system name
     */
    public String jsonifyAllClasses(int limit) throws IOException {
        return jsonifyAllClasses(limit, "Not Specified");
    }

    /**
     * Builds a ClassNode with all its methods, member fields, and relationships
     *
     * @param className the name of the class to build
     * @param packageName the package name of the class
     * @param session the Neo4j session
     * @return a populated ClassNode
     */
    private ClassNode buildClassNode(String className, String packageName, Session session) {
        ClassNode classNode = new ClassNode();
        classNode.setName(className);
        classNode.setPackageName(packageName);

        getInheritance(className, session, classNode);
        getImplementedInterfaces(className, session, classNode);
        getMethods(className, session, classNode);
        getImports(className, session, classNode);
        // getMemberFields(className, session, classNode);

        return classNode;
    }

    // TODO: Trying to remove unnecessary properties from @{link ClassNode}. This code will likely be removed later.
//    /**
//     * Get class fields and their names, types, and access modifiers for the specified class.
//     *
//     * @param className the name of the class
//     * @param session the Neo4j session
//     * @param classNode the ClassNode to populate
//     */
//    private void getMemberFields(String className, Session session, ClassNode classNode) {
//        Result fieldsResult = session.run(CypherConstants.GET_CLASS_FIELDS, Map.of(CypherConstants.PROP_CLASS_NAME, className));
//        while (fieldsResult.hasNext()) {
//            Record record = fieldsResult.next();
//            ClassFieldNode fieldNode = new ClassFieldNode();
//            fieldNode.setName(record.get(CypherConstants.PROP_FIELD_NAME).asString());
//            fieldNode.setType(record.get(CypherConstants.PROP_FIELD_TYPE).asString());
//            fieldNode.setVisibility(record.get(CypherConstants.PROP_VISIBILITY).asString());
//            classNode.getFields().add(fieldNode);
//        }
//    }

    private void getMethods(String className, Session session, ClassNode classNode) {
        Result methodsResult = session.run(CypherConstants.GET_CLASS_METHODS, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (methodsResult.hasNext()) {
            Record record = methodsResult.next();
            String methodName = record.get(CypherConstants.PROP_METHOD_NAME).asString();
            String methodVisibility = record.get(CypherConstants.PROP_METHOD_VISIBILITY).asString("unknown");
            MethodNode methodNode = buildMethodNode(methodName, methodVisibility, session);
            classNode.getMethods().add(methodNode);
        }
    }

    private void getImplementedInterfaces(String className, Session session, ClassNode classNode) {
        Result interfacesResult = session.run(CypherConstants.GET_CLASS_INTERFACES, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (interfacesResult.hasNext()) {
            String interfaceName = interfacesResult.next().get(CypherConstants.PROP_INTERFACE_NAME).asString();
            classNode.getImplementedInterfaces().add(interfaceName);
        }
    }

    private void getInheritance(String className, Session session, ClassNode classNode) {
        Result inheritanceResult = session.run(CypherConstants.GET_CLASS_INHERITANCE, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (inheritanceResult.hasNext()) {
            String parentName = inheritanceResult.next().get(CypherConstants.PROP_PARENT_NAME).asString();
            classNode.getExtendedClasses().add(parentName);
        }
    }

    private void getImports(String className, Session session, ClassNode classNode) {
        Result importsResult = session.run(CypherConstants.GET_CLASS_IMPORTS, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (importsResult.hasNext()) {
            String importName = importsResult.next().get(CypherConstants.PROP_IMPORT_NAME).asString();
            classNode.getImports().add(importName);
        }
    }

    /**
     * Builds a MethodNode with its calls and control flow
     *
     * A MethodNode represents a method with its own method calls and control flow.
     *
     * @param methodName the name of the method to build
     * @param methodVisibility the visibility of the method
     * @param session the Neo4j session
     * @return a fully populated MethodNode
     */
    private MethodNode buildMethodNode(String methodName, String methodVisibility, Session session) {
        MethodNode methodNode = new MethodNode();
        methodNode.setName(methodName);
        methodNode.setVisibility(methodVisibility);

        getMethodCalls(methodName, session, methodNode);
//        getControlFlow(methodName, session, methodNode);

        return methodNode;
    }

    // TODO: Trying to remove unnecessary properties from @{link ClassNode}. This code will likely be removed later.
//    private void getControlFlow(String methodName, Session session, MethodNode methodNode) {
//        Result controlFlowResult = session.run(CypherConstants.GET_CONTROL_FLOW, Map.of(CypherConstants.PROP_METHOD_NAME, methodName));
//        while (controlFlowResult.hasNext()) {
//            Record record = controlFlowResult.next();
//            ControlFlowNode controlFlowNode = new ControlFlowNode();
//            controlFlowNode.setType(record.get(CypherConstants.PROP_TYPE).asString());
//            controlFlowNode.setCondition(record.get(CypherConstants.PROP_CONDITION).asString());
//            methodNode.getControlFlow().add(controlFlowNode);
//        }
//    }

    private void getMethodCalls(String methodName, Session session, MethodNode methodNode) {
        Result callsResult = session.run(CypherConstants.GET_METHOD_CALLS, Map.of(CypherConstants.PROP_METHOD_NAME, methodName));
        while (callsResult.hasNext()) {
            String calledMethod = callsResult.next().get(CypherConstants.PROP_CALLED_METHOD).asString();
            methodNode.getMethodCalls().add(new MethodCallNode(calledMethod));
        }
    }

    public static String getTopLevelNodesAsJSONString() throws IOException {
        return getTopLevelNodesAsJSONString("Unnamed System");
    }
    
    public static String getTopLevelNodesAsJSONString(String systemName) throws IOException {
        try (GraphDatabaseOperations dbOps = new GraphDatabaseOperations()) {
            GraphDataToJsonConverter graphDataToJsonConverter = new GraphDataToJsonConverter(dbOps.getDriver());

            // Get the most significant classes as JSON
            String json = graphDataToJsonConverter.jsonifyMostSignificantClasses(3, systemName);
    
            return json;
        }
    }
}
