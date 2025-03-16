package com.sdg.graph.testdata;

public class ChildClass extends ParentClass {
    private String childField;
    
    public ChildClass(String parentField, String childField) {
        super(parentField);
        this.childField = childField;
    }
    
    @Override
    public void doSomething() {
        System.out.println("Child doing something");
        super.doSomething();
    }
    
    public String getChildField() {
        return childField;
    }
}
