package com.texteditor.ui;

import com.texteditor.io.FileHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Main window class for the text editor application.
 * Handles UI creation, styling, and event handling.
 */
public class TextEditorWindow {
    private TextArea textArea;  // The main text editing component
    private String fileName = "Untitled document 1";  // Default document name

    // Color constants for the dark theme - defined as class constants for easy modification
    private static final String BACKGROUND_COLOR = "#2D2D2D";  // Dark grey for background
    private static final String TEXT_COLOR = "#FFFFFF";        // White for text
    private static final String MENU_BACKGROUND = "#333333";   // Slightly lighter grey for menus

    /**
     * Initializes and displays the text editor window
     * @param stage The primary stage (window) for the application
     */
    public void start(Stage stage) {
        // Create the main text area for document editing
        textArea = new TextArea();

        // Create the root layout container using BorderPane
        // BorderPane divides the screen into 5 regions: top, bottom, left, right, center
        BorderPane root = new BorderPane();

        // Apply styling to the text area for dark theme
        // -fx-control-inner-background: Sets the background color of the editing area
        // -fx-text-fill: Sets the color of the text
        // -fx-font-family: Sets a monospace font for better text editing
        // -fx-font-size: Sets the font size to 14px
        textArea.setStyle(
                "-fx-control-inner-background: " + BACKGROUND_COLOR + ";" +
                        "-fx-text-fill: " + TEXT_COLOR + ";" +
                        "-fx-font-family: 'Monospace';" +
                        "-fx-font-size: 14px;"
        );

        // Create the menu bar with File and Edit menus
        MenuBar menuBar = createMenuBar(stage);

        // Set the menu bar at the top of the BorderPane
        root.setTop(menuBar);

        // Set the text area in the center region (will expand to fill available space)
        root.setCenter(textArea);

        // Create a scene with the root pane and set dimensions (800x600 pixels)
        Scene scene = new Scene(root, 800, 600);

        // Apply CSS styles from external file to the entire scene
        // This allows for more comprehensive styling of all components
        scene.getStylesheets().add(createGlobalStyle());

        // Set the window title to the current file name
        stage.setTitle(fileName);

        // Attach the scene to the stage
        stage.setScene(scene);

        // Show the window
        stage.show();
    }

    /**
     * Creates and configures the menu bar with File and Edit menus
     * @param stage Reference to the main stage for dialogs
     * @return A configured MenuBar with all menu items
     */
    private MenuBar createMenuBar(Stage stage) {
        // Create the main menu bar
        MenuBar menuBar = new MenuBar();

        // Apply dark theme styling to the menu bar
        menuBar.setStyle("-fx-background-color: " + MENU_BACKGROUND + ";");

        // Create the File menu
        Menu fileMenu = new Menu("File");

        // Create "New" menu item to start a new document
        MenuItem newItem = new MenuItem("New");
        newItem.setOnAction(e -> {
            // Clear the text area
            textArea.clear();
            // Reset the file name to default
            fileName = "Untitled document 1";
            // Update the window title
            stage.setTitle(fileName);
        });

        // Create "Open" menu item to open existing files
        MenuItem openItem = new MenuItem("Open");
        openItem.setOnAction(e -> {
            // Create a file chooser dialog for opening files
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open File");



            // Show the open file dialog and get the selected file
            File file = fileChooser.showOpenDialog(stage);

            // If a file was selected (not canceled)
            if (file != null) {
                // Read the content from the file using FileHandler
                String content = FileHandler.readFromFile(file);
                // Set the content in the text area
                textArea.setText(content);
                // Update the file name
                fileName = file.getName();
                // Update the window title with the file name
                stage.setTitle(fileName);
            }
        });

        // Create "Save" menu item to save the current document
        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(e -> {
            // Create a file chooser dialog for saving files
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");

            // Add file filters for saving
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            // Show the save file dialog and get the destination file
            File file = fileChooser.showSaveDialog(stage);

            // If a file was selected (not canceled)
            if (file != null) {
                // Write the text area content to the file using FileHandler
                FileHandler.writeToFile(file, textArea.getText());
                // Update the file name
                fileName = file.getName();
                // Update the window title with the new file name
                stage.setTitle(fileName);
            }
        });

        // Create "Exit" menu item to close the application
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());  // Simply close the window when clicked

        // Create the Edit menu for text manipulation operations
        Menu editMenu = new Menu("Edit");

        // Create "Cut" menu item
        MenuItem cutItem = new MenuItem("Cut");
        // Use the built-in cut functionality of the TextArea
        cutItem.setOnAction(e -> textArea.cut());

        // Create "Copy" menu item
        MenuItem copyItem = new MenuItem("Copy");
        // Use the built-in copy functionality of the TextArea
        copyItem.setOnAction(e -> textArea.copy());

        // Create "Paste" menu item
        MenuItem pasteItem = new MenuItem("Paste");
        // Use the built-in paste functionality of the TextArea
        pasteItem.setOnAction(e -> textArea.paste());

        // Add keyboard shortcuts (accelerators) for common operations
        // This allows users to use familiar key combinations like Ctrl+S to save
        cutItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+X"));
        copyItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+C"));
        pasteItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+V"));
        saveItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+S"));
        openItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+O"));
        newItem.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("Ctrl+N"));

        // Add all items to the File menu
        // SeparatorMenuItem adds a visual line separator between menu items
        fileMenu.getItems().addAll(newItem, openItem, saveItem, new SeparatorMenuItem(), exitItem);

        // Add all items to the Edit menu
        editMenu.getItems().addAll(cutItem, copyItem, pasteItem);

        // Add both menus to the menu bar
        menuBar.getMenus().addAll(fileMenu, editMenu);

        // Return the fully configured menu bar
        return menuBar;
    }

    /**
     * Returns the path to the CSS file for styling the application
     * Note: This assumes you have a CSS file at the specified location in your resources
     * @return The URL of the CSS file as a string
     */
    private String createGlobalStyle() {
        // Get the URL of the CSS file from the resources folder and convert to string
        return getClass().getResource("/styles/dark-theme.css").toExternalForm();
    }
}