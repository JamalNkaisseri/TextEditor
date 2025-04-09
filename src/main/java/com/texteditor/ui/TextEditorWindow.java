package com.texteditor.ui;

import com.texteditor.io.FileHandler;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;

import static javafx.application.Application.STYLESHEET_MODENA;
import static javafx.application.Application.setUserAgentStylesheet;

/**
 * Main window class for the text editor application.
 * Handles UI creation, styling, and event handling.
 */
public class TextEditorWindow {
    private CodeArea codeArea;  // Using CodeArea instead of TextArea for syntax highlighting
    private String fileName = "Untitled document 1";  // Default document name
    private PythonSyntaxHighlighter pythonHighlighter;

    // Color constants for the dark theme - defined as class constants for easy modification
    private static final String BACKGROUND_COLOR = "#2D2D2D";  // Dark grey for background
    private static final String TEXT_COLOR = "#FFFFFF";        // White for text

    /**
     * Initializes and displays the text editor window
     * @param stage The primary stage (window) for the application
     */
    public void start(Stage stage) {
        // Add this to your start() method before creating the scene
        Platform.setImplicitExit(true);
        setUserAgentStylesheet(STYLESHEET_MODENA); // Force load it once
        setUserAgentStylesheet(null); // Then disable it

        // Create the main code area for document editing with syntax highlighting
        codeArea = new CodeArea();

        // Add line numbers to the left of the code area
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        // Create and apply Python syntax highlighting
        pythonHighlighter = new PythonSyntaxHighlighter();
        pythonHighlighter.applyHighlighting(codeArea);

        // Create the root layout container using BorderPane
        BorderPane root = new BorderPane();

        // Apply styling to the code area for dark theme
        codeArea.setStyle(
                "-fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "-fx-font-family: 'Monospace';" +
                        "-fx-font-size: 14px;"
        );

        // Set the code area in the center region (will expand to fill available space)
        root.setCenter(codeArea);

        // Create a scene with the root pane and set dimensions (800x600 pixels)
        Scene scene = new Scene(root, 800, 600);

        // Set up keyboard shortcuts directly on the scene
        setupKeyboardShortcuts(scene, stage);

        // Apply CSS styles from external file to the entire scene
        scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());

        // Set the window title to the current file name
        stage.setTitle(fileName);

        // Attach the scene to the stage
        stage.setScene(scene);

        // Trigger initial syntax highlighting by inserting empty string
        codeArea.appendText("");

        // Show the window
        stage.show();
    }

    /**
     * Sets up keyboard shortcuts for common operations
     * @param scene The scene to which shortcuts will be added
     * @param stage Reference to the main stage for dialogs
     */
    private void setupKeyboardShortcuts(Scene scene, Stage stage) {
        // Create key combinations
        KeyCombination keyCombNew = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombOpen = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCut = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombExit = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);

        // Add the keyboard shortcuts to the scene
        scene.getAccelerators().put(keyCombNew, () -> {
            // Clear the code area
            codeArea.clear();
            // Reset the file name to default
            fileName = "Untitled document 1";
            // Update the window title
            stage.setTitle(fileName);
        });

        scene.getAccelerators().put(keyCombOpen, () -> {
            // Create a file chooser dialog for opening files
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open File");

            // Add filter for Python files
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Python Files", "*.py"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            // Show the open file dialog and get the selected file
            File file = fileChooser.showOpenDialog(stage);

            // If a file was selected (not canceled)
            if (file != null) {
                // Read the content from the file using FileHandler
                String content = FileHandler.readFromFile(file);
                // Set the content in the code area
                codeArea.replaceText(content);
                // Update the file name
                fileName = file.getName();
                // Update the window title with the file name
                stage.setTitle(fileName);
            }
        });

        scene.getAccelerators().put(keyCombSave, () -> {
            // Create a file chooser dialog for saving files
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save File");

            // Add file filters for saving
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Python Files", "*.py"),
                    new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            // Show the save file dialog and get the destination file
            File file = fileChooser.showSaveDialog(stage);

            // If a file was selected (not canceled)
            if (file != null) {
                // Write the code area content to the file using FileHandler
                FileHandler.writeToFile(file, codeArea.getText());
                // Update the file name
                fileName = file.getName();
                // Update the window title with the new file name
                stage.setTitle(fileName);
            }
        });

        // Add clipboard operations
        scene.getAccelerators().put(keyCombCut, () -> codeArea.cut());
        scene.getAccelerators().put(keyCombCopy, () -> codeArea.copy());
        scene.getAccelerators().put(keyCombPaste, () -> codeArea.paste());

        // Add exit shortcut (Ctrl+Q)
        scene.getAccelerators().put(keyCombExit, () -> stage.close());
    }
}