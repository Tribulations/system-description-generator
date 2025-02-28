//package test;
//
//import com.sdg.model.InputHandler;
//
//import org.
//import java.io.IOException;
//import java.nio.file.*;
//import java.util.List;
//
//
//class InputHandlerTest {
//    private InputHandler inputHandler;
//    private Path tempDir;
//    private Path javaFile;
//    private Path nonJavaFile;
//
//    //@BeforeEach
//    void setUp() throws IOException {
//        inputHandler = new InputHandler();
//
//        // Create a temporary test directory
//        tempDir = Files.createTempDirectory("sdg-test");
//
//        // Create a sample Java file
//        javaFile = tempDir.resolve("TestFile.java");
//        Files.writeString(javaFile, "public class TestFile {}");
//
//        // Create a non-Java file
//        nonJavaFile = tempDir.resolve("README.txt");
//        Files.writeString(nonJavaFile, "This is a test.");
//    }
//
//    //@AfterEach
//    void tearDown() throws IOException {
//        Files.walk(tempDir)
//                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
//                .forEach(path -> {
//                    try {
//                        Files.deleteIfExists(path);
//                    } catch (IOException ignored) { }
//                });
//    }
//
//    //@Test
//    void testProcessSingleJavaFile() {
//        List<Path> result = inputHandler.processInput(javaFile.toString());
//        assertEquals(1, result.size());
//        assertTrue(result.get(0).toString().endsWith(".java"));
//    }
//
//    @Test
//    void testProcessDirectoryWithJavaFiles() {
//        List<Path> result = inputHandler.processInput(tempDir.toString());
//        assertEquals(1, result.size()); // Should find only the Java file
//        assertTrue(result.get(0).toString().endsWith(".java"));
//    }
//
//    @Test
//    void testProcessInvalidInput() {
//        List<Path> result = inputHandler.processInput(nonJavaFile.toString());
//        assertTrue(result.isEmpty());
//    }
//
//    @Test
//    void testProcessEmptyInput() {
//        List<Path> result = inputHandler.processInput("");
//        assertTrue(result.isEmpty());
//    }
//}
