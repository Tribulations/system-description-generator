package com.sdg.graph.testdata;

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
