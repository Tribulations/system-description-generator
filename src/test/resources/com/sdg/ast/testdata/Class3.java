package com.sdg.ast.testdata;

public class Class3 {
    public Long method3(String message, int a, int b) {
        System.out.println("Class3 received: " + message + ", a=" + a + ", b=" + b);

        return Long.valueOf(55);
    }
}
