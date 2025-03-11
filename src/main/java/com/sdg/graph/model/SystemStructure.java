package com.sdg.graph.model;

import java.util.ArrayList;
import java.util.List;

public class SystemStructure {
    private List<ClassNode> classes = new ArrayList<>();

    public SystemStructure() {}

    public void addClass(ClassNode classNode) {
        classes.add(classNode);
    }

    public List<ClassNode> getClasses() {
        return classes;
    }

    public void setClasses(List<ClassNode> classes) {
        this.classes = classes;
    }
}
