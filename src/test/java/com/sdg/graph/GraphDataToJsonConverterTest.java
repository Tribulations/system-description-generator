package com.sdg.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.Objects;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphDataToJsonConverterTest {
    private KnowledgeGraphService knowledgeGraphService;
    private GraphDataToJsonConverter graphDataToJsonConverter;
    private GraphDatabaseOperations dbOps;
    private ObjectMapper mapper;
    private JsonNode classes;

    // Test class names
    private static final String CHILD_CLASS = "ChildClass";
    private static final String PARENT_CLASS = "ParentClass";
    private static final String TEST_INTERFACE = "TestInterface";

    // Test resource paths
    private static final String TEST_RESOURCES_PATH = "com/sdg/graph/testdata/";
    private static final String INTERFACE_FILE = TEST_RESOURCES_PATH + "TestInterface.java";
    private static final String PARENT_FILE = TEST_RESOURCES_PATH + "ParentClass.java";
    private static final String CHILD_FILE = TEST_RESOURCES_PATH + "ChildClass.java";

    // JSON property names from model classes
    private static final String PROP_CLASSES = "classes";
    private static final String PROP_NAME = "name";
    private static final String PROP_EXTENDED_CLASSES = "extendedClasses";
    private static final String PROP_IMPLEMENTED_INTERFACES = "implementedInterfaces";
    private static final String PROP_METHODS = "methods";
    private static final String PROP_IMPORTS = "imports";

    @BeforeEach
    void setUp() {
        dbOps = new GraphDatabaseOperations();
        graphDataToJsonConverter = new GraphDataToJsonConverter(dbOps.getDriver());
        knowledgeGraphService = new KnowledgeGraphService();
        mapper = new ObjectMapper();
        dbOps.deleteAllData(); // Start with a clean database, once per test as per memory guidance
    }

    @AfterEach
    void tearDown() {
        dbOps.deleteAllData();
        dbOps.close();
    }

    private void processTestFiles() throws Exception {
        // Get test file paths using ClassLoader with proper package structure
        ClassLoader classLoader = getClass().getClassLoader();
        URL interfaceUrl = Objects.requireNonNull(classLoader.getResource(INTERFACE_FILE),
                "Test resource not found: " + INTERFACE_FILE);
        URL parentUrl = Objects.requireNonNull(classLoader.getResource(PARENT_FILE),
                "Test resource not found: " + PARENT_FILE);
        URL childUrl = Objects.requireNonNull(classLoader.getResource(CHILD_FILE),
                "Test resource not found: " + CHILD_FILE);

        // Convert URLs to paths
        String interfacePath = Paths.get(interfaceUrl.toURI()).toString();
        String parentPath = Paths.get(parentUrl.toURI()).toString();
        String childPath = Paths.get(childUrl.toURI()).toString();

        // Process each file and wait for completion
        knowledgeGraphService.processKnowledgeGraph(interfacePath)
            .blockingSubscribe();
        knowledgeGraphService.processKnowledgeGraph(parentPath)
            .blockingSubscribe();
        knowledgeGraphService.processKnowledgeGraph(childPath)
            .blockingSubscribe();

        // Give Neo4j a moment to process all transactions
        TimeUnit.MILLISECONDS.sleep(500);

        // Get JSON representation and parse it
        String json = graphDataToJsonConverter.jsonifyAllClasses(3);
        JsonNode root = mapper.readTree(json);
        
        // Store classes array for test methods
        classes = root.get(PROP_CLASSES);
        assertNotNull(classes, "Classes array should be present");
        assertTrue(classes.isArray(), "Classes should be an array");
    }

    @Test
    void testJsonStructure() throws Exception {
        processTestFiles();
    }

    @Test
    void testChildClassPresence() throws Exception {
        processTestFiles();
        
        // Find ChildClass node
        JsonNode childClass = null;
        for (JsonNode classNode : classes) {
            if (classNode.get(PROP_NAME).asText().equals(CHILD_CLASS)) {
                childClass = classNode;
                break;
            }
        }
        
        assertNotNull(childClass, "ChildClass should be present in the JSON");
    }

    @Test
    void testChildClassInheritance() throws Exception {
        processTestFiles();
        
        // Find ChildClass node
        JsonNode childClass = null;
        for (JsonNode classNode : classes) {
            if (classNode.get(PROP_NAME).asText().equals(CHILD_CLASS)) {
                childClass = classNode;
                break;
            }
        }
        
        assertNotNull(childClass, "ChildClass should be present in the JSON");
        
        // Verify inheritance
        JsonNode extendedClasses = childClass.get(PROP_EXTENDED_CLASSES);
        assertNotNull(extendedClasses, "Extended classes array should be present");
        assertTrue(extendedClasses.isArray(), "Extended classes should be an array");
        assertTrue(!extendedClasses.isEmpty(), "ChildClass should have at least one extended class");
        assertEquals(PARENT_CLASS, extendedClasses.get(0).asText(), "ChildClass should extend ParentClass");
    }

    @Test
    void testParentClassPresence() throws Exception {
        processTestFiles();
        
        // Find ParentClass node
        JsonNode parentClass = null;
        for (JsonNode classNode : classes) {
            if (classNode.get(PROP_NAME).asText().equals(PARENT_CLASS)) {
                parentClass = classNode;
                break;
            }
        }
        
        assertNotNull(parentClass, "ParentClass should be present in the JSON");
    }

    @Test
    void testParentClassImplementsInterface() throws Exception {
        processTestFiles();
        
        // Find ParentClass node
        JsonNode parentClass = null;
        for (JsonNode classNode : classes) {
            if (classNode.get(PROP_NAME).asText().equals(PARENT_CLASS)) {
                parentClass = classNode;
                break;
            }
        }
        
        assertNotNull(parentClass, "ParentClass should be present in the JSON");
        
        // Verify interface implementation
        JsonNode implementedInterfaces = parentClass.get(PROP_IMPLEMENTED_INTERFACES);
        assertNotNull(implementedInterfaces,
                "Implemented interfaces array should be present");
        assertTrue(implementedInterfaces.isArray(),
                "Implemented interfaces should be an array");
        assertTrue(implementedInterfaces.size() > 0,
                "ParentClass should have at least one implemented interface");
        assertEquals(TEST_INTERFACE, implementedInterfaces.get(0).asText(),
                "ParentClass should implement TestInterface");
    }

    @Test
    void testMethodsPresence() throws Exception {
        processTestFiles();
        
        // Find both classes
        JsonNode parentClass = null;
        JsonNode childClass = null;
        for (JsonNode classNode : classes) {
            String name = classNode.get(PROP_NAME).asText();
            if (name.equals(PARENT_CLASS)) {
                parentClass = classNode;
            } else if (name.equals(CHILD_CLASS)) {
                childClass = classNode;
            }
        }
        
        assertNotNull(parentClass, "ParentClass should be present in the JSON");
        assertNotNull(childClass, "ChildClass should be present in the JSON");
        
        // Verify methods are present
        JsonNode parentMethods = parentClass.get(PROP_METHODS);
        assertNotNull(parentMethods, "Methods array should be present in ParentClass");
        assertTrue(parentMethods.isArray(), "Methods should be an array");
        assertTrue(!parentMethods.isEmpty(), "ParentClass should have at least one method");
        
        JsonNode childMethods = childClass.get(PROP_METHODS);
        assertNotNull(childMethods, "Methods array should be present in ChildClass");
        assertTrue(childMethods.isArray(), "Methods should be an array");
        assertTrue(!childMethods.isEmpty(), "ChildClass should have at least one method");
    }

    @Test
    void testClassImports() throws Exception {
        processTestFiles();

        List<String> expectedImports = List.of(
            "java.util.List",
            "java.util.ArrayList",
            "java.io.Serializable"
        );

        // Get updated JSON and parse it
        String json = graphDataToJsonConverter.jsonifyAllClasses(3);
        JsonNode root = mapper.readTree(json);
        JsonNode updatedClasses = root.get(PROP_CLASSES);
        
        // Find ParentClass node
        JsonNode parentClass = null;
        for (JsonNode classNode : updatedClasses) {
            if (classNode.get(PROP_NAME).asText().equals(PARENT_CLASS)) {
                parentClass = classNode;
                break;
            }
        }
        
        assertNotNull(parentClass, "ParentClass should be present in the JSON");
        
        // Verify imports
        JsonNode imports = parentClass.get(PROP_IMPORTS);
        assertNotNull(imports, "Imports array should be present");
        assertTrue(imports.isArray(), "Imports should be an array");
        assertEquals(expectedImports.size(), imports.size(), 
            "Should have correct number of imports");
        
        // Check each expected import is present
        for (String expectedImport : expectedImports) {
            boolean found = false;
            for (JsonNode importNode : imports) {
                if (expectedImport.equals(importNode.asText())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Should find import: " + expectedImport);
        }
    }
}
