package com.sdg.io;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class OutputPaths {
    private OutputPaths() {}

    public static Path getBaseOutputDir(String systemName) {
        return Paths.get("sdg-out", systemName);
    }

    public static Path getDiagramsDir(String systemName) {
        return getBaseOutputDir(systemName).resolve("uml-diagrams");
    }
}
