package com.sdg.graph.model;

import java.util.ArrayList;
import java.util.List;

public class MethodNode {
    private String name;
    private List<MethodCallNode> methodCalls = new ArrayList<>();
    private List<ControlFlowNode> controlFlow = new ArrayList<>();

    public MethodNode() {}

    public MethodNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
