package com.sdg.ast.testdata;

public class Class1 {
    public void method1(String message) {
        // Call a method from Class2
        Class2 class2 = new Class2();
        String result = class2.method2();
        new Class3().method3(result, 10, 20);
        new Class3().method3(result, 1, 4555);

        new Class4().method1(message, 10, 20);
    }

    public void notCalledMethod(float a) {
        System.out.println("This method is not called");
    }
}