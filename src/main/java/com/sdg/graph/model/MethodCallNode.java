package com.sdg.graph.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sdg.logging.LoggerUtil;

import java.io.IOException;
import java.util.List;

public class MethodCallNode {
    private String calledMethodName;

    public MethodCallNode() {}

    public MethodCallNode(String calledMethodName) {
        this.calledMethodName = calledMethodName;
    }

    public String getCalledMethodName() {
        return calledMethodName;
    }

    public void setCalledMethodName(String calledMethodName) {
        this.calledMethodName = calledMethodName;
    }

    public static class MethodCallListSerializer extends JsonSerializer<List<MethodCallNode>> {
        @Override
        public void serialize(List<MethodCallNode> methodCallNodes, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            try {
                gen.writeStartArray();
                for (MethodCallNode methodCallNode : methodCallNodes) {
                    gen.writeString(methodCallNode.getCalledMethodName());
                }
                gen.writeEndArray();
            } catch (Exception e) {
                LoggerUtil.debug(getClass(), "Error serializing MethodCallNode list", e);
                throw new IOException("Error serializing MethodCallNode list", e);
            }
        }
    }
}
