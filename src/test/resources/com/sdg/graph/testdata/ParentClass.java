package com.sdg.graph.testdata;

import java.util.List;
import java.util.ArrayList;
import java.io.Serializable;

public class ParentClass implements TestInterface {
    private String parentField;
    
    public ParentClass(String parentField) {
        this.parentField = parentField;
    }
    
    @Override
    public void doSomething() {
        System.out.println("Parent doing something");

        // Only called methods gets insertet to the knowledge graph
        ParentClass parentClass = new ParentClass("parentField");
        parentClass.getParentField();
    }
    
    public String getParentField() {
        return parentField;
    }
}
