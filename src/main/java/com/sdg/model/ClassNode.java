package com.sdg.model;

import java.util.ArrayList;
import java.util.List;

public class ClassNode {
    private String name;
    private List<MethodNode> methods = new ArrayList<>();

    public ClassNode() {}

    public ClassNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodNode> methods) {
        this.methods = methods;
    }
}
