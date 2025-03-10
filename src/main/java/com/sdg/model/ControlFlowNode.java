package com.sdg.model;

public class ControlFlowNode {
    private String method;
    private String type;
    private String condition;

    public ControlFlowNode() {}

    public ControlFlowNode(String method, String type, String condition) {
        this.method = method;
        this.type = type;
        this.condition = condition;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }
}
