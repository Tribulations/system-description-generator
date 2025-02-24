package com.sdg.graph;

/**
 * Test class to test the KnowledgeGraphService class.
 */
public class TestClass {
    private String name;
    private int value;

    public TestClass(String name) {
        this.name = name;
        this.value = 0;
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
        
        // If statement
        if (test.name.length() > 5) {
            System.out.println("Long name");
        } else {
            System.out.println("Short name");
        }
        
        // For loop
        for (int i = 0; i < 3; i++) {
            System.out.println("Count: " + i);
        }
        
        // While loop
        int j = 0;
        while (j < 2) {
            System.out.println("While: " + j);
            j++;
        }
        
        // Switch
        switch (test.name.length() % 3) {
            case 0: System.out.println("Divisible by 3"); break;
            case 1: System.out.println("Remainder 1"); break;
            default: System.out.println("Remainder 2"); break;
        }
    }
}
