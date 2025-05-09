package com.sdg.ast;

import org.junit.jupiter.api.Test;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodCallAnalyzerTest {
    // Test resource paths
    private static final String TEST_RESOURCES_PATH = "com/sdg/ast/testdata/";
    private static final String TEST_RESOURCES_PATH2 = "com/sdg/ast/test/";
    private static final String CLASS1 = TEST_RESOURCES_PATH + "Class1.java";
    private static final String CLASS2 = TEST_RESOURCES_PATH + "Class2.java";
    private static final String CLASS3 = TEST_RESOURCES_PATH + "Class3.java";
    private static final String CLASS4 = TEST_RESOURCES_PATH + "Class4.java";
    private static final String CLASS5 = TEST_RESOURCES_PATH + "package1/Class5.java";
    private static final String CLASS6 = TEST_RESOURCES_PATH2 + "Class6.java";

    // Fully qualified method names
    private static final String CLASS1_METHOD1 = "com.sdg.ast.testdata.Class1.method1(java.lang.String)";
    private static final String CLASS1_NOT_CALLED_METHOD = "com.sdg.ast.testdata.Class1.notCalledMethod(float)";
    private static final String CLASS2_METHOD2 = "com.sdg.ast.testdata.Class2.method2()";
    private static final String CLASS3_METHOD3 = "com.sdg.ast.testdata.Class3.method3(java.lang.String, int, int)";
    private static final String CLASS4_METHOD1 = "com.sdg.ast.testdata.Class4.method1(java.lang.String, int, int)";
    private static final String CLASS4_METHOD2 = "com.sdg.ast.testdata.Class4.method2()";
    private static final String CLASS4_METHOD3 = "com.sdg.ast.testdata.Class4.method3(java.lang.String)";
    private static final String CLASS5_METHOD1 = "com.sdg.ast.testdata.package1.Class5.method1()";
    private static final String CLASS6_METHOD1 = "com.sdg.ast.test.Class6.method1()";

    private Map<String, Integer> analyzeMethodCalls() throws Exception {
        List<String> testFiles = getTestFiles();
        MethodCallAnalyzer analyzer = new MethodCallAnalyzer();
        analyzer.analyze(testFiles, "src");
        return analyzer.getMethodCallsMap();
    }

    @Test
    void testMethodCallResultsAreNotEmpty() throws Exception {
        Map<String, Integer> results = analyzeMethodCalls();
        assertFalse(results.isEmpty());
    }

    @Test
    void testMethodsAreIdentified() throws Exception {
        Map<String, Integer> results = analyzeMethodCalls();

        assertNotNull(results.get(CLASS3_METHOD3));
        assertNotNull(results.get(CLASS2_METHOD2));
    }

    @Test
    void testStandardLibraryMethodsAreNotIncluded() throws Exception {
        Map<String, Integer> results = analyzeMethodCalls();
        String method1 = "java.io.PrintStream.println(java.lang.String)";
        String method2 = "java.lang.Long.valueOf(long)";

        assertFalse(results.containsKey(method1));
        assertFalse(results.containsKey(method2));
    }

    @Test
    void testNotCalledMethodsAreNotIncluded() throws Exception {
        Map<String, Integer> results = analyzeMethodCalls();

        assertFalse(results.containsKey(CLASS1_METHOD1));
        assertFalse(results.containsKey(CLASS1_NOT_CALLED_METHOD));
        assertFalse(results.containsKey(CLASS4_METHOD3));
    }

    @Test
    void testMethodCallCounts() throws Exception {
        Map<String, Integer> results = analyzeMethodCalls();

        int methodCallCount2 = results.get(CLASS2_METHOD2);
        int methodCallCount3 = results.get(CLASS3_METHOD3);
        int methodCallCount4 = results.get(CLASS4_METHOD1);
        int methodCallCount5 = results.get(CLASS4_METHOD2);
        int methodCallCount6 = results.get(CLASS5_METHOD1);
        int methodCallCount7 = results.get(CLASS6_METHOD1);

        assertEquals(2, methodCallCount2);
        assertEquals(3, methodCallCount3);
        assertEquals(2, methodCallCount4);
        assertEquals(1, methodCallCount5);
        assertEquals(1, methodCallCount6);
        assertEquals(1, methodCallCount7);
    }

    @Test
    void testMethodsWithSameNameAreIdentified() throws Exception {
        Map<String, Integer> results = analyzeMethodCalls();

        int method1CallCount = results.get(CLASS2_METHOD2);
        int method2CallCount = results.get(CLASS4_METHOD2);

        assertNotNull(results.get(CLASS2_METHOD2));
        assertNotNull(results.get(CLASS4_METHOD2));
        assertTrue(results.containsKey(CLASS2_METHOD2));
        assertTrue(results.containsKey(CLASS4_METHOD2));
        assertEquals(2, method1CallCount);
        assertEquals(1, method2CallCount);
    }

    private List<String> getTestFiles() throws Exception {
        // Get test file paths using ClassLoader with proper package structure
        ClassLoader classLoader = getClass().getClassLoader();
        URL class1Url = Objects.requireNonNull(classLoader.getResource(CLASS1),
                "Test resource not found: " + CLASS1);
        URL class2Url = Objects.requireNonNull(classLoader.getResource(CLASS2),
                "Test resource not found: " + CLASS2);
        URL class3Url = Objects.requireNonNull(classLoader.getResource(CLASS3),
                "Test resource not found: " + CLASS3);
        URL class4Url = Objects.requireNonNull(classLoader.getResource(CLASS4),
                "Test resource not found: " + CLASS4);
        URL class5Url = Objects.requireNonNull(classLoader.getResource(CLASS5),
                "Test resource not found: " + CLASS5);
        URL class6Url = Objects.requireNonNull(classLoader.getResource(CLASS6),
                "Test resource not found: " + CLASS6);

        // Convert URLs to paths
        String class1Path = Paths.get(class1Url.toURI()).toString();
        String class2Path = Paths.get(class2Url.toURI()).toString();
        String class3Path = Paths.get(class3Url.toURI()).toString();
        String class4Path = Paths.get(class4Url.toURI()).toString();
        String class5Path = Paths.get(class5Url.toURI()).toString();
        String class6Path = Paths.get(class6Url.toURI()).toString();

        return Arrays.asList(class1Path, class2Path, class3Path, class4Path, class5Path, class6Path);
    }
}
