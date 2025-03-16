package com.sdg.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.driver.Values.parameters;

class GraphDatabaseOperationsTest {
    private GraphDatabaseOperations dbOps;
    private static final String CHILD_CLASS = "ChildClass";
    private static final String PARENT_CLASS = "ParentClass";
    private static final String TEST_INTERFACE = "TestInterface";

    @BeforeEach
    void setUp() {
        dbOps = new GraphDatabaseOperations();
        dbOps.deleteAllData(); // Start with a clean database
    }

    @AfterEach
    void tearDown() {
        dbOps.deleteAllData();
        dbOps.close();
    }

    @Test
    void testCreateClassNodes() {
        // Create the class nodes
        dbOps.createClassNode(CHILD_CLASS);
        dbOps.createClassNode(PARENT_CLASS);

        // Verify nodes were created
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(
                String.format("MATCH (c:Class) RETURN c.%s as name", CypherConstants.PROP_CLASS_NAME)
            );
            List<String> classNames = result.list(record -> record.get("name").asString());
            
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

        // Verify inheritance relationship
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(
                String.format("MATCH (child:Class {%s: $childName})-[:EXTENDS]->(parent:Class {%s: $parentName}) RETURN count(*) as count",
                    CypherConstants.PROP_CLASS_NAME, CypherConstants.PROP_CLASS_NAME),
                parameters("childName", CHILD_CLASS, "parentName", PARENT_CLASS)
            );
            long relationshipCount = result.single().get("count").asLong();
            assertEquals(1, relationshipCount, "Should have one inheritance relationship");
        }
    }

    @Test
    void testCreateInterfaceImplementation() {
        // Create the class node, interface node, and implementation relationship
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createInterfaceImplementation(PARENT_CLASS, TEST_INTERFACE);

        // Verify interface node and implementation relationship
        try (Session session = dbOps.getDriver().session()) {
            Result interfaceResult = session.run(
                String.format("MATCH (i:Interface {%s: $interfaceName}) RETURN count(*) as count",
                    CypherConstants.PROP_INTERFACE_NAME),
                parameters("interfaceName", TEST_INTERFACE)
            );
            assertEquals(1, interfaceResult.single().get("count").asLong(), "Should have one interface node");

            Result implementsResult = session.run(
                String.format("MATCH (c:Class {%s: $className})-[:IMPLEMENTS]->(i:Interface {%s: $interfaceName}) RETURN count(*) as count",
                    CypherConstants.PROP_CLASS_NAME, CypherConstants.PROP_INTERFACE_NAME),
                parameters("className", PARENT_CLASS, "interfaceName", TEST_INTERFACE)
            );
            assertEquals(1, implementsResult.single().get("count").asLong(), "Should have one implements relationship");
        }
    }

    @Test
    void testCreateMethodNodes() {
        // Create class and method nodes
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createMethodNode(PARENT_CLASS, "testMethod");
        dbOps.createMethodNode(PARENT_CLASS, "anotherMethod");

        // Verify method nodes and their relationships to the class
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(
                String.format("MATCH (c:Class {%s: $className})-[:HAS_METHOD]->(m:Method) RETURN m.%s as name",
                    CypherConstants.PROP_CLASS_NAME, CypherConstants.PROP_METHOD_NAME),
                parameters("className", PARENT_CLASS)
            );
            var methodNames = result.list(record -> record.get("name").asString());
            
            assertEquals(2, methodNames.size(), "Should have exactly two methods");
            assertTrue(methodNames.contains("testMethod"), "testMethod should exist");
            assertTrue(methodNames.contains("anotherMethod"), "anotherMethod should exist");
        }
    }

    @Test
    void testCreateClassField() {
        // Create class and field nodes
        dbOps.createClassNode(PARENT_CLASS);
        dbOps.createClassField(PARENT_CLASS, "testField", "String", "private");

        // Verify field node and its relationship to the class
        try (Session session = dbOps.getDriver().session()) {
            Result result = session.run(
                String.format("MATCH (c:Class {%s: $className})-[:HAS_FIELD]->(f:ClassField) " +
                "RETURN f.%s as name, f.%s as type, f.%s as visibility",
                    CypherConstants.PROP_CLASS_NAME,
                    CypherConstants.PROP_FIELD_NAME,
                    CypherConstants.PROP_FIELD_TYPE,
                    CypherConstants.PROP_VISIBILITY),
                parameters("className", PARENT_CLASS)
            );
            var record = result.single();
            
            assertEquals("testField", record.get("name").asString(), "Field name should match");
            assertEquals("String", record.get("type").asString(), "Field type should match");
            assertEquals("private", record.get("visibility").asString(), "Field visibility should match");
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
        dbOps.createMethodNode(PARENT_CLASS, "parentMethod");
        dbOps.createMethodNode(CHILD_CLASS, "childMethod");
        
        // Add fields
        dbOps.createClassField(PARENT_CLASS, "parentField", "String", "protected");
        dbOps.createClassField(CHILD_CLASS, "childField", "int", "private");

        // Verify the complete structure
        try (Session session = dbOps.getDriver().session()) {
            // Verify class hierarchy
            Result inheritanceResult = session.run(
                String.format("MATCH (child:Class {%s: $childName})-[:EXTENDS]->(parent:Class {%s: $parentName}) " +
                "MATCH (parent)-[:IMPLEMENTS]->(interface:Interface {%s: $interfaceName}) " +
                "RETURN count(*) as count",
                    CypherConstants.PROP_CLASS_NAME,
                    CypherConstants.PROP_CLASS_NAME,
                    CypherConstants.PROP_INTERFACE_NAME),
                parameters(
                    "childName", CHILD_CLASS,
                    "parentName", PARENT_CLASS,
                    "interfaceName", TEST_INTERFACE
                )
            );
            assertEquals(1, inheritanceResult.single().get("count").asLong(), 
                "Should have complete inheritance and interface implementation structure");

            // Verify methods
            Result methodResult = session.run(
                String.format("MATCH (c:Class)-[:HAS_METHOD]->(m:Method) " +
                "RETURN c.%s as className, collect(m.%s) as methods",
                    CypherConstants.PROP_CLASS_NAME,
                    CypherConstants.PROP_METHOD_NAME)
            );
            var methodRecords = methodResult.list();
            assertEquals(2, methodRecords.size(), "Should have methods in both classes");

            // Verify fields
            Result fieldResult = session.run(
                String.format("MATCH (c:Class)-[:HAS_FIELD]->(f:ClassField) " +
                "RETURN c.%s as className, collect(f.%s) as fields",
                    CypherConstants.PROP_CLASS_NAME,
                    CypherConstants.PROP_FIELD_NAME)
            );
            var fieldRecords = fieldResult.list();
            assertEquals(2, fieldRecords.size(), "Should have fields in both classes");
        }
    }
}
