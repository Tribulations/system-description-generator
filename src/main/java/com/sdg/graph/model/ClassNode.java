package com.sdg.graph.model;

import java.util.ArrayList;
import java.util.List;

public class ClassNode {
    private String name;
    private List<String> extendedClasses = new ArrayList<>();
    private List<String> implementedInterfaces = new ArrayList<>();
    private List<MethodNode> methods = new ArrayList<>();
    private List<ClassFieldNode> fields = new ArrayList<>();
    private List<String> imports = new ArrayList<>();

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

    public List<String> getExtendedClasses() {
        return extendedClasses;
    }

    public void setExtendedClasses(List<String> extendedClasses) {
        this.extendedClasses = extendedClasses;
    }

    public List<String> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public void setImplementedInterfaces(List<String> implementedInterfaces) {
        this.implementedInterfaces = implementedInterfaces;
    }

    public List<MethodNode> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodNode> methods) {
        this.methods = methods;
    }

    public List<ClassFieldNode> getFields() {
        return fields;
    }

    public void setFields(List<ClassFieldNode> fields) {
        this.fields = fields;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }
}
