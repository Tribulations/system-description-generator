package com.sdg.diagrams;

import com.sdg.logging.LoggerUtil;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PlantUMLValidator {
    public static boolean validatePlantUMLSyntax(String plantUMLSource) throws IOException {
        try {
            SourceStringReader plantUmlReader = new SourceStringReader(plantUMLSource);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileFormatOption errorFormatOption = new FileFormatOption(FileFormat.UTXT);
            plantUmlReader.outputImage(baos, errorFormatOption);

            if (baos.toString().contains("Error")) {
                LoggerUtil.info(PlantUMLValidator.class,
                        "PlantUML Syntax Error with error message {} for plant uml:\n{}", baos.toString(), plantUMLSource);
                throw new RuntimeException(baos.toString());
            }

            // Check if we got any output
            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length == 0) {
                throw new RuntimeException("PlantUML produced no output - check diagram syntax");
            }

        } catch (IOException e) {
            throw new IOException(e.getCause());
        } catch (IllegalArgumentException e) {
            LoggerUtil.info(PlantUMLValidator.class, "Could not create byte array output stream when validating plant uml: \n{}", plantUMLSource);
            throw e;
        }

        return true;
    }
}
