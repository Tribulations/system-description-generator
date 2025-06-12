package com.sdg.graph.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

public class MethodNode {
    private String name;
    private String visibility;
    private String returnType;
    private String parameters;

    @JsonSerialize(using = MethodCallNode.MethodCallListSerializer.class)
    private List<MethodCallNode> methodCalls = new ArrayList<>();

    public MethodNode() {}

    public MethodNode(String name) {
        this.name = name;
    }

    public MethodNode(String name, String visibility) {
        this.name = name;
        this.visibility = visibility;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public List<MethodCallNode> getMethodCalls() {
        return methodCalls;
    }

    public void setMethodCalls(List<MethodCallNode> methodCalls) {
        this.methodCalls = methodCalls;
    }
}
