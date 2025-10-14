package com.sdg.diagrams;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;

import java.awt.image.BufferedImage;

/**
 * A tab for a single PlantUML diagram.
 *
 * @author Joakim Colloz
 */
public class DiagramTab {
    private final String name;
    private final String plantUMLSource;
    private JPanel diagramPanel;
    private boolean rendered = false;

    public DiagramTab(String name, String plantUMLSource) {
        this.name = name;
        this.plantUMLSource = plantUMLSource;
        initializeTab();
    }

    private void initializeTab() {
        diagramPanel = new JPanel(new BorderLayout());
        diagramPanel.setPreferredSize(new Dimension(600, 500));
        diagramPanel.setBackground(Color.WHITE);
    }

    public JPanel getDiagramTab() {
        return diagramPanel;
    }

    public String getPlantUMLSource() {
        return plantUMLSource;
    }

    public boolean isRendered() {
        return rendered;
    }

    public void setRendered(boolean rendered) {
        this.rendered = rendered;
    }

    public void displayImage(BufferedImage image) {
        diagramPanel.removeAll();

        JLabel imageLabel = new JLabel(new ImageIcon(image));
        diagramPanel.setLayout(new BorderLayout());
        diagramPanel.add(imageLabel, BorderLayout.CENTER);

        diagramPanel.revalidate();
        diagramPanel.repaint();
    }

    public void displayErrorMessage(String message) {
        diagramPanel.removeAll();

        JLabel errorLabel = new JLabel("<html><center>" + message + "</center></html>");
        errorLabel.setForeground(Color.RED);
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        diagramPanel.setLayout(new BorderLayout());
        diagramPanel.add(errorLabel, BorderLayout.CENTER);

        diagramPanel.revalidate();
        diagramPanel.repaint();
    }
}
