package com.sdg.graph;

/**
 * Test class to test the KnowledgeGraphService class.
 */
public class TestClass {
    private String name;

    public TestClass(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        TestClass test = new TestClass("JavaParser Test");
        test.setName("New name");
        System.out.println(test.getName());
    }
}
