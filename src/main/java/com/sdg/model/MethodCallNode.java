package com.sdg.model;

public class MethodCallNode {
    private String calledMethod;

    public MethodCallNode() {}

    public MethodCallNode(String calledMethod) {
        this.calledMethod = calledMethod;
    }

    public String getCalledMethod() {
        return calledMethod;
    }

    public void setCalledMethod(String calledMethod) {
        this.calledMethod = calledMethod;
    }
}
