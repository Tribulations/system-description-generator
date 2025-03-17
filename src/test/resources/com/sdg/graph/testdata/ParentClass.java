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
    }
    
    public String getParentField() {
        return parentField;
    }
}
