package com.sdg.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.neo4j.driver.Values.parameters;

/**
 * This class contains unit tests for the {@link GraphDatabaseOperations} class.
 */
class GraphDatabaseOperationsTest {
    private GraphDatabaseOperations dbOps;
    private static final String CHILD_CLASS = "ChildClass";
    private static final String PARENT_CLASS = "ParentClass";
    private static final String TEST_INTERFACE = "TestInterface";

    @BeforeEach
    void setUp() {
        dbOps = new GraphDatabaseOperations();
        dbOps.deleteAllData(); // Start with a clean database for each test
        dbOps.startBatchSession();
        dbOps.startBatchTransaction();
    }

    @AfterEach
    void tearDown() {
        if (dbOps.isBatchTransactionActive()) {
            dbOps.commitBatchTransaction();
        }
        if (dbOps.isBatchSessionActive()) {
            dbOps.endBatchSession();
        }
        dbOps.close();
    }

    @Test
    void testBatchTransactionRequired() {
        // First test that operations work with an active batch transaction
        assertDoesNotThrow(() -> dbOps.createClassNode("TestClass"), 
            "Should not throw exception when batch transaction is active");
        
        // Commit the current transaction
        dbOps.commitBatchTransaction();
        
        // Now test that operations throw exceptions when no batch transaction is active
        assertThrows(IllegalStateException.class, 
            () -> dbOps.createClassNode("AnotherClass"),
            "Should throw IllegalStateException when no batch transaction is active");
    }
    
    @Test
    void testBatchSessionRequired() {
        // First test that operations work with an active batch session
        assertDoesNotThrow(() -> dbOps.startBatchTransaction(), 
            "Should not throw exception when batch session is active");
        
        // End the current session (and transaction)
        dbOps.commitBatchTransaction();
        dbOps.endBatchSession();
        
        // Now test that operations throw exceptions when no batch session is active
        assertThrows(IllegalStateException.class, 
            () -> dbOps.startBatchTransaction(),
            "Should throw IllegalStateException when no batch session is active");
    }

    @Test
    void testCreateClassNodes() {
        // Create the class nodes
        dbOps.createClassNode(CHILD_CLASS);
        dbOps.createClassNode(PARENT_CLASS);

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify nodes were created
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(CypherConstants.FIND_ALL_CLASSES);
            List<String> classNames = result.list(record -> record.get(CypherConstants.PROP_CLASS_NAME).asString());
            
            assertTrue(classNames.contains(CHILD_CLASS), "ChildClass node should exist");
            assertTrue(classNames.contains(PARENT_CLASS), "ParentClass node should exist");
            assertEquals(2, classNames.size(), "Should have exactly two class nodes");
        }
    }

    @Test
    void testCreateInheritanceRelationship() {
        // Create the class nodes and inheritance relationship
        dbOps.createClassNode(CHILD_CLASS);
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createInheritanceRelationship(CHILD_CLASS, PARENT_CLASS);

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify inheritance relationship
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(CypherConstants.GET_CLASS_INHERITANCE,
                parameters(CypherConstants.PROP_CLASS_NAME, CHILD_CLASS));
            String parentName = result.single().get(CypherConstants.PROP_PARENT_NAME).asString();
            assertEquals(PARENT_CLASS, parentName, "Should have correct inheritance relationship");
        }
    }

    @Test
    void testCreateInterfaceImplementation() {
        // Create the class node, interface node, and implementation relationship
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createInterfaceImplementation(PARENT_CLASS, TEST_INTERFACE);

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify interface implementation
        try (Session session = dbOps.getDriver().session()) {
            Result interfaceResult = session.run(CypherConstants.GET_CLASS_INTERFACES,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));
            String interfaceName = interfaceResult.single().get(CypherConstants.PROP_INTERFACE_NAME).asString();
            assertEquals(TEST_INTERFACE, interfaceName, "Should have correct interface implementation");
        }
    }

    @Test
    void testCreateMethodNodes() {
        // Create class and method nodes
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createMethodNode(PARENT_CLASS, "testMethod", "public", "void",
                "String, int");
        dbOps.createMethodNode(PARENT_CLASS, "anotherMethod", "private", "Long",
               "float, int");

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify method nodes and their relationships to the class
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(CypherConstants.GET_CLASS_METHODS,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));

            // Extract method names and access modifiers from the records
            List<Record> records = result.list();
            List<String> methodNames = records.stream()
                .map(record -> record.get(CypherConstants.PROP_METHOD_NAME).asString())
                .toList();

            List<String> methodAccessModifiers = records.stream()
                .map(record -> record.get(CypherConstants.PROP_METHOD_VISIBILITY).asString())
                .toList();

            // Verify method nodes
            assertEquals(2, methodNames.size(), "Should have exactly two methods");
            assertEquals(2, methodAccessModifiers.size(),
                    "Both methods should have an access modifier");
            assertTrue(methodNames.contains("testMethod"), "testMethod should exist");
            assertTrue(methodNames.contains("anotherMethod"), "anotherMethod should exist");
            
            // Find the index of each method to check its corresponding access modifier
            int testMethodIndex = methodNames.indexOf("testMethod");
            int anotherMethodIndex = methodNames.indexOf("anotherMethod");
            
            assertEquals("public", methodAccessModifiers.get(testMethodIndex),
                    "testMethod should be public");
            assertEquals("private", methodAccessModifiers.get(anotherMethodIndex),
                    "anotherMethod should be private");
        }
    }

    @Test
    void testCreateClassField() {
        // Create class and field nodes
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createClassField(PARENT_CLASS, "testField", "String", "private");

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify field node and its relationship to the class
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(CypherConstants.GET_CLASS_FIELDS,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));
            Record record = result.single();
            
            assertEquals("testField", record.get(CypherConstants.PROP_FIELD_NAME).asString(), "Field name should match");
            assertEquals("String", record.get(CypherConstants.PROP_FIELD_TYPE).asString(), "Field type should match");
            assertEquals("private", record.get(CypherConstants.PROP_VISIBILITY).asString(), "Field visibility should match");
        }
    }

    @Test
    void testCreateImportRelationship() {
        // Create class and import nodes
        String importName = "java.util.List";
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createImportRelationship(PARENT_CLASS, importName);

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify import relationship
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(CypherConstants.GET_CLASS_IMPORTS,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));
            String foundImport = result.single().get(CypherConstants.PROP_IMPORT_NAME).asString();
            assertEquals(importName, foundImport, "Should have correct import relationship");
        }
    }

    @Test
    void testMultipleImports() {
        // Create class and multiple imports
        List<String> imports = List.of(
            "java.util.List",
            "java.util.Map",
            "java.io.File"
        );

        dbOps.createClassNode(PARENT_CLASS);
        imports.forEach(importName -> dbOps.createImportRelationship(PARENT_CLASS, importName));

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify all imports
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(CypherConstants.GET_CLASS_IMPORTS,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));
            List<String> foundImports = result.list(record -> record.get(CypherConstants.PROP_IMPORT_NAME).asString());
            assertEquals(imports.size(), foundImports.size(), "Should have correct number of imports");
            imports.forEach(importName -> 
                assertTrue(foundImports.contains(importName), "Should contain import: " + importName));
        }
    }

    @Test
    void testCompleteClassStructure() {
        // Create the complete structure of classes, interface, and relationships
        dbOps.createClassNode(CHILD_CLASS);
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createInheritanceRelationship(CHILD_CLASS, PARENT_CLASS);
        dbOps.createInterfaceImplementation(PARENT_CLASS, TEST_INTERFACE);
        
        // Add methods to both classes
        dbOps.createMethodNode(PARENT_CLASS, "parentMethod", "public", "JsonNode",
                "String, double");
        dbOps.createMethodNode(CHILD_CLASS, "childMethod", "private", "String",
                "String, float");
        
        // Add fields
        dbOps.createClassField(PARENT_CLASS, "parentField", "String", "protected");
        dbOps.createClassField(CHILD_CLASS, "childField", "int", "private");

        // Commit the transaction to make changes visible to queries
        dbOps.commitBatchTransaction();
        
        // Verify the complete structure
        try (Session session = dbOps.getDriver().session()) {
            // Verify class inheritance
            Result inheritanceResult = session.run(CypherConstants.GET_CLASS_INHERITANCE,
                parameters(CypherConstants.PROP_CLASS_NAME, CHILD_CLASS));
            String parentName = inheritanceResult.single().get(CypherConstants.PROP_PARENT_NAME).asString();
            assertEquals(PARENT_CLASS, parentName, "Should have correct inheritance relationship");

            // Verify interface implementation
            Result interfaceResult = session.run(CypherConstants.GET_CLASS_INTERFACES,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));
            String interfaceName = interfaceResult.single().get(CypherConstants.PROP_INTERFACE_NAME).asString();
            assertEquals(TEST_INTERFACE, interfaceName, "Should have correct interface implementation");

            // Verify methods
            Result parentMethodResult = session.run(CypherConstants.GET_CLASS_METHODS,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));
            List<String> parentMethods = parentMethodResult.list(record -> 
                record.get(CypherConstants.PROP_METHOD_NAME).asString());
            assertEquals(1, parentMethods.size(), "ParentClass should have one method");
            assertTrue(parentMethods.contains("parentMethod"), "Should find parentMethod");

            Result childMethodResult = session.run(CypherConstants.GET_CLASS_METHODS,
                parameters(CypherConstants.PROP_CLASS_NAME, CHILD_CLASS));
            List<String> childMethods = childMethodResult.list(record -> 
                record.get(CypherConstants.PROP_METHOD_NAME).asString());
            assertEquals(1, childMethods.size(), "ChildClass should have one method");
            assertTrue(childMethods.contains("childMethod"), "Should find childMethod");

            // Verify fields
            Result parentFieldResult = session.run(CypherConstants.GET_CLASS_FIELDS,
                parameters(CypherConstants.PROP_CLASS_NAME, PARENT_CLASS));
            Record parentField = parentFieldResult.single();
            assertEquals("parentField", parentField.get(CypherConstants.PROP_FIELD_NAME).asString(), 
                "Should find parentField");
            assertEquals("String", parentField.get(CypherConstants.PROP_FIELD_TYPE).asString(), 
                "Should have correct field type");
            assertEquals("protected", parentField.get(CypherConstants.PROP_VISIBILITY).asString(), 
                "Should have correct visibility");

            Result childFieldResult = session.run(CypherConstants.GET_CLASS_FIELDS,
                parameters(CypherConstants.PROP_CLASS_NAME, CHILD_CLASS));
            Record childField = childFieldResult.single();
            assertEquals("childField", childField.get(CypherConstants.PROP_FIELD_NAME).asString(), 
                "Should find childField");
            assertEquals("int", childField.get(CypherConstants.PROP_FIELD_TYPE).asString(), 
                "Should have correct field type");
            assertEquals("private", childField.get(CypherConstants.PROP_VISIBILITY).asString(), 
                "Should have correct visibility");
        }
    }
}
