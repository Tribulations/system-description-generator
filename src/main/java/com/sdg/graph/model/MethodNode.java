package com.sdg.graph.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

public class MethodNode {
    private String methodSignature;

    @JsonSerialize(using = MethodCallNode.MethodCallListSerializer.class)
    private List<MethodCallNode> methodCalls = new ArrayList<>();

    public MethodNode() {}

    public String getMethodSignature() {
        return methodSignature;
    }

    public void setMethodSignature(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public List<MethodCallNode> getMethodCalls() {
        return methodCalls;
    }

    public void setMethodCalls(List<MethodCallNode> methodCalls) {
        this.methodCalls = methodCalls;
    }
}
