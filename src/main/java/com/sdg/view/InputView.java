package com.sdg.view;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.GridBagConstraints;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.ActionListener;

/**
 * The InputView class represents the graphical user interface for the application.
 * It allows users to select an input file and process it while displaying the results.
 *
 * @author Suraj Karki
 */
public class InputView extends JFrame {
    public final JTextField inputField;
    private final JButton browseButton;
    private final JButton processButton;
    private final JButton descButton;
    private final JTextArea outputArea;

    /**
     * Constructs the InputView UI, setting up layout and components.
     */
    public InputView() {
        setTitle("File Input Handler");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel with GridBagLayout for flexible resizing
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        inputField = new JTextField(25); // Text field for input path
        browseButton = new JButton("Browse");
        processButton = new JButton("Insert to Graph");
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
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Add Components
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true);
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
}