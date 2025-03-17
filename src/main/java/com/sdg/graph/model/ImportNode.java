package com.sdg.graph.model;

/**
 * Represents an import statement in a Java class.
 * This node type helps track dependencies between classes through their imports.
 */
public class ImportNode {
    private String name;

    public ImportNode() {}

    public ImportNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
