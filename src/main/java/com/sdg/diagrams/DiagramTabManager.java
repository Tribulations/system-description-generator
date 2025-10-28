package com.sdg.diagrams;

import com.sdg.logging.LoggerUtil;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

import javax.imageio.ImageIO;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * A manager/tab containing multiple PlantUML {@link DiagramTab}s.
 *
 * @author Joakim Colloz
 */
public class DiagramTabManager extends JPanel {
    private JTabbedPane tabbedPane;
    private final Map<String, DiagramTab> diagramTabs;
    private final Map<String, DiagramResult> diagramResults; // holds success or failure per tab
    private final List<String> diagramOrder = new ArrayList<>();

    public DiagramTabManager(List<DiagramResult> results, boolean useResults) {
        diagramTabs = new HashMap<>();
        diagramResults = new HashMap<>();
        initializeUIFromResults(results);
    }

    private void initializeUIFromResults(List<DiagramResult> results) {
        setLayout(new BorderLayout());

        // Create tabbed pane for diagrams
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        // Initialize tabs for each diagram
        initializeDiagramTabsFromResults(results);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initializeDiagramTabsFromResults(List<DiagramResult> results) {
        final String[] DIAGRAM_NAMES = new String[results.size()];
        for (int i = 0; i < results.size(); i++) {
            String diagramName = "Diagram " + (i + 1);
            DIAGRAM_NAMES[i] = diagramName;

            DiagramResult result = results.get(i);
            DiagramTab tab = new DiagramTab(diagramName, result.getPlantUMLSource());
            diagramTabs.put(diagramName, tab);
            diagramResults.put(diagramName, result);

            tabbedPane.addTab(diagramName, tab.getDiagramTab());
            diagramOrder.add(diagramName);
        }

        // Render the first tab initially
        if (!diagramTabs.isEmpty()) {
            String firstName = DIAGRAM_NAMES[0];
            DiagramTab firstTab = diagramTabs.get(firstName);
            renderDiagram(firstName, firstTab);
        }

        // Add tab change listener to render diagrams when switched
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < DIAGRAM_NAMES.length) {
                String selectedDiagramName = DIAGRAM_NAMES[selectedIndex];
                DiagramTab selectedTab = diagramTabs.get(selectedDiagramName);
                if (selectedTab != null && !selectedTab.isRendered()) {
                    renderDiagram(selectedDiagramName, selectedTab);
                }
            }
        });
    }

    private void renderDiagram(String name, DiagramTab tab) {
        SwingUtilities.invokeLater(() -> {
            try {
                DiagramResult result = diagramResults.get(name);
                if (result != null && !result.isSuccess()) {
                    // Show provided failure message
                    tab.displayErrorMessage(result.getErrorMessage());
                } else {
                    BufferedImage image = renderPlantUMLDiagram(tab.getPlantUMLSource());
                    if (image != null) {
                        tab.displayImage(image);
                    } else {
                        tab.displayErrorMessage("Failed to render PlantUML diagram as tab is null.");
                        LoggerUtil.error(DiagramTabManager.class, "Failed to render PlantUML diagram as tab is null.");
                    }
                }

                tab.setRendered(true);

            } catch (Exception e) {
                tab.displayErrorMessage("Error rendering diagram: " + e.getMessage());
                LoggerUtil.error(DiagramTabManager.class, "Error rendering diagram: " + e.getMessage());
            }
        });
    }

    /**
     * Render PlantUML diagram using PlantUML library
     */
    private BufferedImage renderPlantUMLDiagram(String plantUMLSource) {
        try {
            LoggerUtil.info(DiagramTabManager.class, "Rendering PlantUML diagram...");

            SourceStringReader plantUmlReader = new SourceStringReader(plantUMLSource);

            // Render to ByteArrayOutputStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileFormatOption formatOption = new FileFormatOption(FileFormat.PNG);
            plantUmlReader.outputImage(baos, formatOption);

            // Check if we got any output
            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length == 0) {
                LoggerUtil.error(DiagramTabManager.class, "PlantUML produced no output - check diagram syntax");
                return null;
            }

            // Convert to BufferedImage
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bais);

            if (image == null) {
                LoggerUtil.error(DiagramTabManager.class, "Failed to convert PlantUML output to BufferedImage as image is null");
            }

            LoggerUtil.info(DiagramTabManager.class, "PlantUML rendered successfully, image size: {} bytes", imageBytes.length);

            return image;

        } catch (IllegalArgumentException e) {
            LoggerUtil.error(DiagramTabManager.class, "ByteArrayInputStream is null. PlantUML rendering failed: " + e.getMessage());
            return null;
        } catch (IOException e) {
            LoggerUtil.error(DiagramTabManager.class,
                    "Creating Buffered image failed.Error when reading BufferedImage or failure to create ImageInputStreamPlantUML." + e.getMessage());
            return null;
        } catch (RuntimeException e) {
            LoggerUtil.error(DiagramTabManager.class, "PlantUML rendering failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Render PlantUML to PNG bytes using PlantUML library.
     */
    private byte[] renderPlantUMLDiagramToPngBytes(String plantUMLSource) throws IOException {
        try {
            SourceStringReader plantUmlReader = new SourceStringReader(plantUMLSource);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileFormatOption formatOption = new FileFormatOption(FileFormat.PNG);
            plantUmlReader.outputImage(baos, formatOption);

            byte[] imageBytes = baos.toByteArray();
            if (imageBytes.length == 0) {
                LoggerUtil.error(DiagramTabManager.class, "PlantUML produced no output - check diagram syntax");
                throw new IOException("PlantUML produced no output");
            }
            return imageBytes;
        } catch (RuntimeException e) {
            LoggerUtil.error(DiagramTabManager.class, "PlantUML rendering failed: " + e.getMessage());
            throw new IOException("PlantUML rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Saves all successfully generated diagrams to the specified directory as PNG files.
     * Filenames are generated as diagram-<index>.png starting at 1.
     * Failed diagrams are skipped.
     */
    public void saveAllDiagramsAsPng(Path outputDir) throws IOException {
        if (outputDir == null) throw new IllegalArgumentException("outputDir cannot be null");
        Files.createDirectories(outputDir);

        for (int i = 0; i < diagramOrder.size(); i++) {
            String name = diagramOrder.get(i);
            DiagramResult result = diagramResults.get(name);
            if (result == null || !result.isSuccess()) {
                LoggerUtil.info(DiagramTabManager.class, "Skipping saving of failed or missing diagram: {}", name);
                continue;
            }
            String fileName = String.format("diagram-%d.png", i + 1);
            Path file = outputDir.resolve(fileName);
            byte[] pngBytes = renderPlantUMLDiagramToPngBytes(result.getPlantUMLSource());
            Files.write(file, pngBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            LoggerUtil.info(DiagramTabManager.class, "Saved diagram '{}' to {}", name, file.toAbsolutePath());
        }
    }
}
