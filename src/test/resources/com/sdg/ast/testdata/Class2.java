package com.sdg.ast.testdata;

public class Class2 {
    public String method2() {
        // Call a method from Class3
        Class3 class3 = new Class3();
        Long result = class3.method3("test message", 10, 20);
        
        return "Something returned: " + result;
    }
}