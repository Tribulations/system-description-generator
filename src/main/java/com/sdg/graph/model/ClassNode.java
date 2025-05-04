package com.sdg.graph.model;

import java.util.ArrayList;
import java.util.List;

// TODO: rename some of the member fields to better align with relation names
public class ClassNode {
    private String name;
    private String packageName;
    private List<String> extendedClasses = new ArrayList<>();
    private List<String> implementedInterfaces = new ArrayList<>();
    private List<MethodNode> methods = new ArrayList<>(); // TODO : rename e.g., hasMethods
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
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

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }
}
