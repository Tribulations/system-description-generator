package com.sdg.graph.model;

import java.util.ArrayList;
import java.util.List;

public class MethodNode {
    private String name;
    private String visibility;
    private List<MethodCallNode> methodCalls = new ArrayList<>();
    private List<ControlFlowNode> controlFlow = new ArrayList<>();

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

    public List<MethodCallNode> getMethodCalls() {
        return methodCalls;
    }

    public void setMethodCalls(List<MethodCallNode> methodCalls) {
        this.methodCalls = methodCalls;
    }

    public List<ControlFlowNode> getControlFlow() {
        return controlFlow;
    }

    public void setControlFlow(List<ControlFlowNode> controlFlow) {
        this.controlFlow = controlFlow;
    }
}
