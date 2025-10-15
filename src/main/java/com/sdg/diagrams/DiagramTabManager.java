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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import static com.sdg.diagrams.PlantUMLSamples.DIAGRAM_NAMES;

/**
 * A manager/tab containing multiple PlantUML {@link DiagramTab}s.
 *
 * @author Joakim Colloz
 */
public class DiagramTabManager extends JPanel {
    private JTabbedPane tabbedPane;
    private final Map<String, DiagramTab> diagramTabs;

    public DiagramTabManager(List<String> plantUMLSources) {
        diagramTabs = new HashMap<>();
        initializeUI(plantUMLSources);
    }

    private void initializeUI(List<String> plantUMLSources) {
        setLayout(new BorderLayout());

        // Create tabbed pane for diagrams
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);

        // Initialize tabs for each diagram
        initializeDiagramTabs(plantUMLSources);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initializeDiagramTabs(List<String> plantUMLSources) {
        final String[] DIAGRAM_NAMES = new String[plantUMLSources.size()];
        for (int i = 0; i < plantUMLSources.size(); i++) {
            String diagramName = "Diagram " + (i + 1);
            DIAGRAM_NAMES[i] = diagramName;

            DiagramTab tab = new DiagramTab(diagramName, plantUMLSources.get(i));
            diagramTabs.put(diagramName, tab);

            tabbedPane.addTab(diagramName, tab.getDiagramTab());
        }

        // Render the first tab initially
        if (!diagramTabs.isEmpty()) {
            DiagramTab firstTab = diagramTabs.get(DIAGRAM_NAMES[0]);
            renderDiagram(firstTab);
        }

        // Add tab change listener to render diagrams when switched
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0 && selectedIndex < DIAGRAM_NAMES.length) {
                String selectedDiagramName = DIAGRAM_NAMES[selectedIndex];
                DiagramTab selectedTab = diagramTabs.get(selectedDiagramName);
                if (selectedTab != null && !selectedTab.isRendered()) {
                    renderDiagram(selectedTab);
                }
            }
        });
    }

    private void renderDiagram(DiagramTab tab) {
        SwingUtilities.invokeLater(() -> {
            try {
                BufferedImage image = renderPlantUMLDiagram(tab.getPlantUMLSource());

                if (image != null) {
                    tab.displayImage(image);
                } else {
                    tab.displayErrorMessage("Failed to render PlantUML diagram as tab is null.");
                    LoggerUtil.error(DiagramTabManager.class, "Failed to render PlantUML diagram as tab is null.");
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
}
