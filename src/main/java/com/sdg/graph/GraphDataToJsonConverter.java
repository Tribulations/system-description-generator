package com.sdg.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sdg.logging.LoggerUtil;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Driver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
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
 * @version 1.2
 */
public class GraphDataToJsonConverter {
    private final Driver neo4jDriver;
    private final ObjectMapper objectMapper;
    
    // Default character limit for JSON output to not exceed LLM token limit
    private static final int DEFAULT_JSON_CHAR_LIMIT = 150000;

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
         * Classes are added one by one until the character limit is reached;
         * if a class causes the limit to be exceeded, it is removed to ensure the final output stays within the limit.
         *
         * @param classLimit the maximum number of classes to include
         * @param systemName the name of the system being analyzed
         * @param charLimit the maximum number of characters allowed in the JSON output
         * @return JSON string representation of the most important classes
         * @throws IOException if conversion to JSON fails
         */
        public String jsonifyMostSignificantClasses(int classLimit, String systemName, int charLimit) throws IOException {
            SystemStructure system = new SystemStructure(systemName);

            try (Session session = neo4jDriver.session()) {
                Result result = session.run(CypherConstants.FIND_CLASSES_WITH_MOST_RELATIONSHIPS,
                        Map.of(CypherConstants.PROP_LIMIT, classLimit));

                while (result.hasNext()) {
                    Record record = result.next();
                    String className = record.get(CypherConstants.PROP_CLASS_NAME).asString();
                    String packageName = record.get(CypherConstants.PROP_PACKAGE_NAME).asString("<None>");

                    // Build the class node
                    ClassNode classNode = buildClassNode(className, packageName, session);

                    // Add the class to the system temporarily
                    system.addClass(classNode);

                    // Check if adding this class made the JSON exceed the character limit
                    String currentJson = objectMapper.writeValueAsString(system);
                    if (currentJson.length() > charLimit) {
                        // Remove the last added class as the JSON exceeds the character limit
                        system.getClasses().removeLast();
                        LoggerUtil.info(GraphDataToJsonConverter.class,
                                "Character limit reached. Stopping at {} classes.", system.getClasses().size());
                        LoggerUtil.info(GraphDataToJsonConverter.class, "Removed class: {}", className);
                        break;
                    }
                }
            }

            String json = objectMapper.writeValueAsString(system);
            writeJsonToFile(json);
            return json;
        }
    
    /**
     * Extracts the most significant classes from the knowledge graph and converts them to JSON.
     * Uses the default character limit.
     *
     * @param classLimit the maximum number of classes to include
     * @param systemName the name of the system being analyzed
     * @return JSON string representation of the most important classes
     * @throws IOException if conversion to JSON fails
     */
    public String jsonifyMostSignificantClasses(int classLimit, String systemName) throws IOException {
        return jsonifyMostSignificantClasses(classLimit, systemName, DEFAULT_JSON_CHAR_LIMIT);
    }

    /**
     * Overloaded method that uses a default system name and default character limit
     */
    public String jsonifyMostSignificantClasses(int classLimit) throws IOException {
        return jsonifyMostSignificantClasses(classLimit, "Not specified", DEFAULT_JSON_CHAR_LIMIT);
    }
    
    /**
     * Overloaded method that uses a default system name ("Not specified") with specified character limit
     */
    public String jsonifyMostSignificantClasses(int classLimit, int charLimit) throws IOException {
        return jsonifyMostSignificantClasses(classLimit, "Not specified", charLimit);
    }

    public String jsonifyAllClasses(int classLimit, String systemName) throws IOException {
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

        queryInheritance(className, session, classNode);
        queryImplementedInterfaces(className, session, classNode);
        queryMethods(className, session, classNode);
        queryImports(className, session, classNode);
        // getMemberFields(className, session, classNode);

        return classNode;
    }

    private void queryMethods(String className, Session session, ClassNode classNode) {
        Result methodsResult = session.run(CypherConstants.GET_CLASS_METHODS, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (methodsResult.hasNext()) {
            Record record = methodsResult.next();

            // TODO: only include properties that are non-empty
            String methodName = record.get(CypherConstants.PROP_METHOD_NAME).asString();
            String methodVisibility = record.get(CypherConstants.PROP_METHOD_VISIBILITY).asString("unknown");
            String methodReturnType = record.get(CypherConstants.PROP_METHOD_RETURN_TYPE).asString("unknown");
            String methodParameters = record.get(CypherConstants.PROP_METHOD_PARAMETERS).asString("unknown");

            LoggerUtil.debug(getClass(), "Retrieving method: {}, with methodVisibility: {}, methodReturnType: {}, methodParameters: {}", methodName, methodVisibility, methodReturnType, methodParameters);

            MethodNode methodNode = buildMethodNode(methodName, methodVisibility, methodReturnType, methodParameters, session);
            classNode.getMethods().add(methodNode);
        }
    }

    private void queryImplementedInterfaces(String className, Session session, ClassNode classNode) {
        Result interfacesResult = session.run(CypherConstants.GET_CLASS_INTERFACES, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (interfacesResult.hasNext()) {
            String interfaceName = interfacesResult.next().get(CypherConstants.PROP_INTERFACE_NAME).asString();
            classNode.getImplementedInterfaces().add(interfaceName);
        }
    }

    private void queryInheritance(String className, Session session, ClassNode classNode) {
        Result inheritanceResult = session.run(CypherConstants.GET_CLASS_INHERITANCE, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (inheritanceResult.hasNext()) {
            String parentName = inheritanceResult.next().get(CypherConstants.PROP_PARENT_NAME).asString();
            classNode.getExtendedClasses().add(parentName);
        }
    }

    private void queryImports(String className, Session session, ClassNode classNode) {
        Result importsResult = session.run(CypherConstants.GET_CLASS_IMPORTS, Map.of(CypherConstants.PROP_CLASS_NAME, className));
        while (importsResult.hasNext()) {
            String importName = importsResult.next().get(CypherConstants.PROP_IMPORT_NAME).asString();
            classNode.getImports().add(importName);
        }
    }

    /**
     * Builds a MethodNode with its method signature and method calls.
     *
     * @param methodName the name of the method to build
     * @param methodVisibility the visibility of the method
     * @param session the Neo4j session
     * @return a fully populated MethodNode
     */
    private MethodNode buildMethodNode(String methodName, String methodVisibility, String returnType,
                                       String parameters, Session session) {
        String methodSignature = createMethodSignatureString(methodName, methodVisibility, returnType, parameters);
        MethodNode methodNode = new MethodNode();
        methodNode.setMethodSignature(methodSignature);

        buildMethodCalls(methodName, session, methodNode);

        return methodNode;
    }

    private static String createMethodSignatureString(String methodName, String methodVisibility, String returnType, String parameters) {
        // Remove trailing comma from parameters string
        if (parameters.endsWith(", ")) {
            parameters = parameters.substring(0, parameters.length() - 2);
        }

        String methodSignature = methodVisibility +
                " " +
                returnType +
                " " +
                methodName +
                "(" +
                parameters +
                ")";

        return methodSignature;
    }

    private void buildMethodCalls(String methodName, Session session, MethodNode methodNode) {
        Result callsResult = session.run(CypherConstants.GET_METHOD_CALLS, Map.of(CypherConstants.PROP_METHOD_NAME, methodName));
        while (callsResult.hasNext()) {
            String calledMethod = callsResult.next().get(CypherConstants.PROP_CALLED_METHOD).asString();
            methodNode.getMethodCalls().add(new MethodCallNode(calledMethod));
        }
    }

    /**
     * Writes the given JSON string to a file.
     * @param json The JSON string to write.
     */
    private void writeJsonToFile(String json) {
        final String filename = "graph_output.txt";

        try {
            Files.writeString(Paths.get(filename), json, StandardOpenOption.CREATE);
        } catch (IOException e) {
            LoggerUtil.error(getClass(), "Failed to write JSON to file: " + filename, e);
        }
    }

    public static String buildTopLevelNodesAsJSONString() throws IOException {
        return buildTopLevelNodesAsJSONString("Unnamed System");
    }
    
    public static String buildTopLevelNodesAsJSONString(String systemName) throws IOException {
        try (GraphDatabaseOperations dbOps = new GraphDatabaseOperations()) {
            GraphDataToJsonConverter graphDataToJsonConverter = new GraphDataToJsonConverter(dbOps.getDriver());

            // Get the most significant classes as JSON with default character limit
            String json = graphDataToJsonConverter.jsonifyMostSignificantClasses(Integer.MAX_VALUE, systemName);
    
            return json;
        }
    }
}
