package com.sdg.ast.testdata;

public class Class4 {
    public Long method1(String message, int a, int b) {
        return Long.valueOf(55);
    }

    public String method2() {
        // Call a method from Class2
        Class2 class2 = new Class2();
        String result = class2.method2();
        
        return "Class4.method2 got: " + result;
    }

    public int method3(String message) {
        System.out.println(message);
        
        // Call method1 and method2 within the same class
        method1("Internal call to method1", 5, 10);
        String result = method2();
        System.out.println(result);
        
        return 42;
    }
}
