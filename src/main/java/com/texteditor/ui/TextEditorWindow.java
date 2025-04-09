package com.texteditor.ui;

import com.texteditor.io.FileHandler;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static javafx.application.Application.STYLESHEET_MODENA;
import static javafx.application.Application.setUserAgentStylesheet;

/**
 * Main window class for the text editor application.
 * Handles UI creation, styling, and event handling with a tabbed interface.
 */
public class TextEditorWindow {
    // Color constants for the dark theme
    private static final String BACKGROUND_COLOR = "#2D2D2D";         // Dark grey for background
    private static final String TEXT_COLOR = "#FFFFFF";               // White for text
    private static final String TAB_SELECTED_COLOR = "#5A5A5A";       // Lighter grey for selected tab
    private static final String TAB_UNSELECTED_COLOR = "#333333";     // Medium grey for unselected tabs
    private static final String TAB_HEADER_COLOR = "#1A1A1A";         // Very dark grey for tab header area

    private TabPane tabPane;  // TabPane to hold multiple document tabs
    private int untitledCount = 1;  // Counter for untitled documents
    private Map<Tab, EditorTab> tabContents = new HashMap<>();  // Map to store tab data

    /**
     * Initializes and displays the text editor window
     * @param stage The primary stage (window) for the application
     */
    public void start(Stage stage) {
        // Set up platform configuration
        Platform.setImplicitExit(true);
        setUserAgentStylesheet(STYLESHEET_MODENA);
        setUserAgentStylesheet(null);

        // Create the tab pane for multiple documents
        tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);

        // Create the root layout container
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);

        // Create a scene with the root pane
        Scene scene = new Scene(root, 800, 600);

        // Set up keyboard shortcuts
        setupKeyboardShortcuts(scene, stage);

        // Apply CSS styles from external file
        scene.getStylesheets().add(getClass().getResource("/styles/dark-theme.css").toExternalForm());

        // Custom tab styling to match the screenshot with removed blue focus indicator
        String customTabStyle =
                ".tab-pane .tab-header-area .tab-header-background {" +
                        "    -fx-background-color: " + TAB_HEADER_COLOR + ";" + // Very dark header area
                        "    -fx-border-width: 0 0 1 0;" +
                        "    -fx-border-color: #555555;" +
                        "}" +
                        ".tab-pane .tab-header-area {" +
                        "    -fx-padding: 0px;" +
                        "}" +
                        // General tab style with focus colors disabled
                        ".tab-pane .tab {" +
                        "    -fx-focus-color: transparent;" +
                        "    -fx-faint-focus-color: transparent;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab {" +
                        "    -fx-background-color: " + TAB_UNSELECTED_COLOR + ";" +
                        "    -fx-background-radius: 0;" +
                        "    -fx-padding: 3px 15px 3px 15px;" + // Compact padding
                        "    -fx-pref-width: 200px;" + // Fixed width tabs
                        "    -fx-max-width: 200px;" +
                        "    -fx-min-width: 150px;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected {" +
                        "    -fx-background-color: " + TAB_SELECTED_COLOR + ";" +
                        "    -fx-border-width: 0 0 2 0;" +
                        "    -fx-border-color: #FFA500;" + // Orange indicator for selected tab
                        "    -fx-focus-color: transparent;" + // Disable focus highlight
                        "    -fx-faint-focus-color: transparent;" + // Also disable the faint variant
                        "}" +
                        ".tab-pane .tab .tab-label {" +
                        "    -fx-text-fill: #CCCCCC;" +
                        "    -fx-font-size: 13px;" +
                        "}" +
                        ".tab-pane .tab:selected .tab-label {" +
                        "    -fx-text-fill: #FFFFFF;" +
                        "}" +
                        // Clean, subtle close button
                        ".tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-close-button {" +
                        "    -fx-background-color: #CCCCCC;" +
                        "    -fx-shape: \"M 0,0 H1 L 4,3 7,0 H8 V1 L 5,4 8,7 V8 H7 L 4,5 1,8 H0 V7 L 3,4 0,1 Z\";" +
                        "    -fx-scale-shape: false;" +
                        "}";

        scene.getStylesheets().add("data:text/css," + customTabStyle.replace(" ", "%20"));

        // Create initial tabs with empty content
        createNewTab("Untitled 1");
        createNewTab("Untitled 2");

        // Select the first tab
        tabPane.getSelectionModel().select(0);

        // Set the window title
        stage.setTitle("Yoru1chi"); // Set editor name
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Inner class to represent the content of a tab
     */
    private class EditorTab {
        private CodeArea codeArea;
        private String fileName;
        private File file;
        private PythonSyntaxHighlighter highlighter;

        public EditorTab(String fileName) {
            this.fileName = fileName;
            this.file = null;

            // Create the code area
            codeArea = new CodeArea();

            // Add style class for scrollbars
            codeArea.getStyleClass().add("always-visible-scrollbars");

            // Add line numbers
            codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

            // Apply styling
            codeArea.setStyle(
                    "-fx-background-color: " + BACKGROUND_COLOR + ";" +
                            "-fx-font-family: 'Monospace';" +
                            "-fx-font-size: 14px;" +
                            "-fx-text-fill: " + TEXT_COLOR + ";"
            );

            // Enable vertical scrolling and disable word wrap
            codeArea.setWrapText(false);

            // Apply syntax highlighting
            highlighter = new PythonSyntaxHighlighter();
            highlighter.applyHighlighting(codeArea);
        }

        public CodeArea getCodeArea() {
            return codeArea;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }
    }

    /**
     * Creates a new tab with an empty editor
     */
    private Tab createNewTab() {
        String defaultName = "Untitled " + untitledCount++;
        return createNewTab(defaultName);
    }

    /**
     * Creates a new tab with the specified filename
     */
    private Tab createNewTab(String fileName) {
        // Create new editor content
        EditorTab editorTab = new EditorTab(fileName);

        // Create a tab
        Tab tab = new Tab(fileName);
        tab.setContent(editorTab.getCodeArea());

        // Store tab data
        tabContents.put(tab, editorTab);

        // Add tab to pane
        tabPane.getTabs().add(tab);

        // Select the new tab
        tabPane.getSelectionModel().select(tab);

        // Add close handler to remove from map
        tab.setOnClosed(e -> tabContents.remove(tab));

        return tab;
    }

    /**
     * Opens a file in a new tab
     */
    private void openFile(Stage stage) {
        // Create file chooser
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");

        // Add filters
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Python Files", "*.py"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Show dialog
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            // Read content
            String content = FileHandler.readFromFile(file);

            // Create new tab
            Tab tab = createNewTab(file.getName());

            // Get editor tab data
            EditorTab editorTab = tabContents.get(tab);

            // Update file info
            editorTab.setFileName(file.getName());
            editorTab.setFile(file);

            // Set content
            editorTab.getCodeArea().replaceText(content);
        }
    }

    /**
     * Saves the current tab content to a file
     */
    private void saveCurrentTab(Stage stage) {
        // Get current tab
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        if (selectedTab != null) {
            EditorTab editorTab = tabContents.get(selectedTab);

            // Get or create a file
            if (editorTab.getFile() == null) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save File");

                // Add filters
                fileChooser.getExtensionFilters().addAll(
                        new FileChooser.ExtensionFilter("Python Files", "*.py"),
                        new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                        new FileChooser.ExtensionFilter("All Files", "*.*")
                );

                // Show dialog
                File file = fileChooser.showSaveDialog(stage);

                if (file != null) {
                    editorTab.setFile(file);
                    editorTab.setFileName(file.getName());
                    selectedTab.setText(file.getName());
                } else {
                    return; // Canceled
                }
            }

            // Write to file
            FileHandler.writeToFile(editorTab.getFile(), editorTab.getCodeArea().getText());
        }
    }

    /**
     * Gets the active editor tab
     */
    private EditorTab getCurrentEditorTab() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        return currentTab != null ? tabContents.get(currentTab) : null;
    }

    /**
     * Sets up keyboard shortcuts
     */
    private void setupKeyboardShortcuts(Scene scene, Stage stage) {
        // Define shortcuts
        KeyCombination keyCombNew = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombOpen = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCut = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombExit = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCloseTab = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);

        // Create new tab (Ctrl+N)
        scene.getAccelerators().put(keyCombNew, this::createNewTab);

        // Open file (Ctrl+O)
        scene.getAccelerators().put(keyCombOpen, () -> openFile(stage));

        // Save file (Ctrl+S)
        scene.getAccelerators().put(keyCombSave, () -> saveCurrentTab(stage));

        // Cut/Copy/Paste
        scene.getAccelerators().put(keyCombCut, () -> {
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) currentTab.getCodeArea().cut();
        });

        scene.getAccelerators().put(keyCombCopy, () -> {
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) currentTab.getCodeArea().copy();
        });

        scene.getAccelerators().put(keyCombPaste, () -> {
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) currentTab.getCodeArea().paste();
        });

        // Close tab (Ctrl+W)
        scene.getAccelerators().put(keyCombCloseTab, () -> {
            Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
            if (currentTab != null) tabPane.getTabs().remove(currentTab);
        });

        // Exit (Ctrl+Q)
        scene.getAccelerators().put(keyCombExit, () -> stage.close());
    }
}