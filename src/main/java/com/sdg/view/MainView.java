package com.sdg.view;

import com.sdg.diagrams.DiagramTabManager;

import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * The InputView class represents the graphical user interface for the application.
 * It allows users to select an input file and process it while displaying the results.
 *
 * @author Suraj Karki
 * @author Joakim Colloz
 */
public class MainView extends JFrame {
    public final JTextField inputField;
    private final JButton browseButton;
    private final JButton processButton;
    private final JButton descButton;
    private final JTextArea outputArea;
    private final JProgressBar progressBar;
    private final JLabel loadingLabel;
    private final JTabbedPane tabbedPane;

    /**
     * Constructs the InputView UI, setting up layout and components.
     */
    public MainView() {
        setTitle("System Description Generator (SDG)");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel with GridBagLayout for flexible resizing
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        inputField = new JTextField(25); // Text field for input path
        inputField.setForeground(Color.GRAY);
        inputField.setText("Browse your project / Enter GitHub url...");
        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent evt) {
                if (inputField.getText().equals("Browse your project / Enter GitHub url...")) {
                    inputField.setText("");
                    inputField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent evt) {
                if (inputField.getText().isEmpty()) {
                    inputField.setForeground(Color.GRAY);
                    inputField.setText("Browse your project / Enter GitHub url...");
                }
            }
        });

        browseButton = new JButton("Browse");
        processButton = new JButton("Create Knowledge Graph");
        descButton = new JButton("Generate Description");

        // Configure input field to expand and fill available space
        gbc.weightx = 1.0; // Allow inputField to expand
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        inputPanel.add(inputField, gbc);

        // Configure buttons with no expansion
        gbc.weightx = 0; // Reset weight for buttons
        gbc.gridx = 1;
        inputPanel.add(browseButton, gbc);

        gbc.gridx = 2;
        inputPanel.add(processButton, gbc);

        gbc.gridx = 3;
        inputPanel.add(descButton, gbc);

        // Output Area
        outputArea = new JTextArea(10, 40);
        outputArea.setEditable(false);
        outputArea.setBorder(new EmptyBorder(10, 20, 10, 20));
        outputArea.setFont(new Font("Arial", Font.PLAIN, 14));
        // Enable line wrapping
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(outputArea);

        tabbedPane = new JTabbedPane();
        tabbedPane.add("Textual Description", scrollPane);

        // Loading Indicator Panel
        JPanel loadingPanel = new JPanel();
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true); // Enables moving animation
        progressBar.setPreferredSize(new Dimension(600, 30));
        loadingLabel = new JLabel("Processing...");
        loadingLabel.setVisible(false);
        progressBar.setVisible(false);

        loadingPanel.add(loadingLabel);
        loadingPanel.add(progressBar);

        // Add Components
        add(inputPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(loadingPanel, BorderLayout.SOUTH);

        descButton.setEnabled(false);

        setVisible(true);
    }

    public void addDiagramToTabbedPane(String tabTitle, DiagramTabManager diagramTabManager) {
        tabbedPane.add(tabTitle, diagramTabManager);
    }

    /**
     * Retrieves the input path entered by the user.
     *
     * @return The input file path as a string.
     */
    public String getInputPath() {
        return inputField.getText();
    }

    /**
     * Sets the output area text.
     *
     * @param text The text to display in the output area.
     */
    public void setOutputText(String text) {
        outputArea.setText(text);
    }

    /**
     * Appends text to the output area and scrolls to the bottom.
     *
     * @param text The text to append.
     */
    public void appendOutputText(String text) {
        outputArea.append(text);
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    /**
     * Adds an ActionListener to the "Browse" button.
     *
     * @param listener The ActionListener to add.
     */
    public void addBrowseListener(ActionListener listener) {
        browseButton.addActionListener(listener);
    }

    /**
     * Adds an ActionListener to the "Process" button.
     *
     * @param listener The ActionListener to add.
     */
    public void addProcessListener(ActionListener listener) {
        processButton.addActionListener(listener);
    }

    /**
     * Adds an ActionListener to the "Generate description" button.
     *
     * @param listener The ActionListener to add.
     */
    public void addDecListener(ActionListener listener) {
        descButton.addActionListener(listener);
    }

    // Getter methods for buttons
    public JButton getProcessButton() {
        return processButton;
    }

    public JButton getDescButton() {
        return descButton;
    }

    /**
     * Shows the loading indicator.
     *
     * @param message The message to display while loading.
     */
    public void showLoadingIndicator(String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(true);
            loadingLabel.setText(message);
            loadingLabel.setVisible(true);
            revalidate();
            repaint();
        });
    }

    /**
     * Clears the loading indicator once the task is completed.
     */
    public void clearLoadingIndicator() {
        SwingUtilities.invokeLater(() -> {
            loadingLabel.setVisible(false);
            progressBar.setVisible(false);
            revalidate();
            repaint();
        });
    }

    public void clearOutputArea() {
        outputArea.setText("");
    }
}
