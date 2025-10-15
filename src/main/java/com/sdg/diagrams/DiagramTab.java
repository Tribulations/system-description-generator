package com.sdg.diagrams;

import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JScrollPane;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

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
    private ScalableImagePanel imagePanel;

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
        imagePanel = new ScalableImagePanel(image);
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        scrollPane.setBorder(null);
        diagramPanel.setLayout(new BorderLayout());
        diagramPanel.add(scrollPane, BorderLayout.CENTER);

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

    /**
     * Panel that scales an image to fit available space while keeping aspect ratio.
     */
    private static class ScalableImagePanel extends JPanel {
        private final BufferedImage image;

        ScalableImagePanel(BufferedImage image) {
            this.image = image;
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;

            int panelW = getWidth();
            int panelH = getHeight();
            int imgW = image.getWidth();
            int imgH = image.getHeight();

            if (panelW <= 0 || panelH <= 0 || imgW <= 0 || imgH <= 0) return;

            double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
            int drawW = (int) Math.round(imgW * scale);
            int drawH = (int) Math.round(imgH * scale);
            int x = (panelW - drawW) / 2;
            int y = (panelH - drawH) / 2;

            Graphics2D g2d = (Graphics2D) g.create();
            // Render nicely
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(image, x, y, drawW, drawH, null);
            g2d.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(800, 600);
        }
    }
}
