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

        // Only called methods gets insertet to the knowledge graph
        ChildClass childClass = new ChildClass("parentField", "childField");
        childClass.getChildField();
    }
    
    public String getChildField() {
        return childField;
    }
}
