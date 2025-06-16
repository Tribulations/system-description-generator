package com.sdg.ast;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
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

    private Map<String, Integer> mergedMethodCallsMap;

    @BeforeEach
    void setUp() throws Exception {
        List<Path> testFiles = getTestFiles();
        MethodCallAnalyzer analyzer = new MethodCallAnalyzer("src");
        List<Map<String, Integer>> methodCallsMapList = new ArrayList<>();
        testFiles.forEach(path -> methodCallsMapList.add(analyzer.analyze(path)));

        mergedMethodCallsMap = new HashMap<>();

        for (Map<String, Integer> methodCallsMap : methodCallsMapList) {
            methodCallsMap.keySet().forEach(key -> {
                mergedMethodCallsMap.merge(key, methodCallsMap.get(key), Integer::sum);
            });
        }
    }

    @Test
    void testMethodCallResultsAreNotEmpty() throws Exception {
        assertFalse(mergedMethodCallsMap.isEmpty());
    }

    @Test
    void testMethodsAreIdentified() throws Exception {
        assertNotNull(mergedMethodCallsMap.get(CLASS3_METHOD3));
        assertNotNull(mergedMethodCallsMap.get(CLASS2_METHOD2));
    }

    @Test
    void testStandardLibraryMethodsAreNotIncluded() throws Exception {
        String method1 = "java.io.PrintStream.println(java.lang.String)";
        String method2 = "java.lang.Long.valueOf(long)";

        assertFalse(mergedMethodCallsMap.containsKey(method1));
        assertFalse(mergedMethodCallsMap.containsKey(method2));
    }

    @Test
    void testNotCalledMethodsAreNotIncluded() throws Exception {
        assertFalse(mergedMethodCallsMap.containsKey(CLASS1_METHOD1));
        assertFalse(mergedMethodCallsMap.containsKey(CLASS1_NOT_CALLED_METHOD));
        assertFalse(mergedMethodCallsMap.containsKey(CLASS4_METHOD3));
    }

    @Test
    void testMethodCallCounts() throws Exception {
        int methodCallCount2 = mergedMethodCallsMap.get(CLASS2_METHOD2);
        int methodCallCount3 = mergedMethodCallsMap.get(CLASS3_METHOD3);
        int methodCallCount4 = mergedMethodCallsMap.get(CLASS4_METHOD1);
        int methodCallCount5 = mergedMethodCallsMap.get(CLASS4_METHOD2);
        int methodCallCount6 = mergedMethodCallsMap.get(CLASS5_METHOD1);
        int methodCallCount7 = mergedMethodCallsMap.get(CLASS6_METHOD1);

        assertEquals(2, methodCallCount2);
        assertEquals(3, methodCallCount3);
        assertEquals(2, methodCallCount4);
        assertEquals(1, methodCallCount5);
        assertEquals(1, methodCallCount6);
        assertEquals(1, methodCallCount7);
    }

    @Test
    void testMethodsWithSameNameAreIdentified() throws Exception {

        int method1CallCount = mergedMethodCallsMap.get(CLASS2_METHOD2);
        int method2CallCount = mergedMethodCallsMap.get(CLASS4_METHOD2);

        assertNotNull(mergedMethodCallsMap.get(CLASS2_METHOD2));
        assertNotNull(mergedMethodCallsMap.get(CLASS4_METHOD2));
        assertTrue(mergedMethodCallsMap.containsKey(CLASS2_METHOD2));
        assertTrue(mergedMethodCallsMap.containsKey(CLASS4_METHOD2));
        assertEquals(2, method1CallCount);
        assertEquals(1, method2CallCount);
    }

    private List<Path> getTestFiles() throws Exception {
        List<Path> testFiles = new ArrayList<>();
        testFiles.add(Paths.get(getResourcePath(CLASS1)));
        testFiles.add(Paths.get(getResourcePath(CLASS2)));
        testFiles.add(Paths.get(getResourcePath(CLASS3)));
        testFiles.add(Paths.get(getResourcePath(CLASS4)));
        testFiles.add(Paths.get(getResourcePath(CLASS5)));
        testFiles.add(Paths.get(getResourcePath(CLASS6)));
        return testFiles;
    }

    private String getResourcePath(String resourceName) throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceUrl = Objects.requireNonNull(classLoader.getResource(resourceName),
                "Test resource not found: " + resourceName);
        return Paths.get(resourceUrl.toURI()).toString();
    }
}
