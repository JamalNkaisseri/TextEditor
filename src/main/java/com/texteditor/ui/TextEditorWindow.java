package com.texteditor.ui;

import com.texteditor.io.FileHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.io.File;
import java.util.Collections;
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
    private static final String TAB_HEADER_COLOR = "#2D2D2D";         // Very dark grey for tab header area
    private static final String SCROLLBAR_THUMB_COLOR = "#666666";    // Grey for scrollbar thumb
    private static final String SCROLLBAR_TRACK_COLOR = "#3A3A3A";    // Darker grey for scrollbar track
    private static final String CURSOR_COLOR = "#FFA500";             // Orange for cursor
    private static final String CURRENT_LINE_COLOR = "#383838";       // Slightly lighter than background for current line

    private TabPane tabPane;  // TabPane to hold multiple document tabs
    private int untitledCount = 1;  // Counter for untitled documents
    private Map<Tab, EditorTab> tabContents = new HashMap<>();  // Map to store tab data
    private Label statusLabel;  // Status bar to display cursor position

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

        // Set up tab selection handler to focus the editor when tab is selected
        setupTabSelectionHandler();

        // Create status bar for cursor position
        statusLabel = new Label("Line: 1, Column: 1");
        statusLabel.setPadding(new Insets(3, 10, 3, 10));
        statusLabel.setStyle("-fx-background-color: #333333; -fx-text-fill: #BBBBBB;");

        // Create the root layout container
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(statusLabel);

        // Create a scene with the root pane
        Scene scene = new Scene(root, 800, 600);

        // Set up keyboard shortcuts
        setupKeyboardShortcuts(scene, stage);

        // Apply CSS styles with custom scrollbar styling
        applyCustomStyles(scene);

        // Create initial tabs with empty content
        createNewTab("Untitled");

        // Select the first tab
        tabPane.getSelectionModel().select(0);

        // Focus the code area in the first tab
        Platform.runLater(() -> {
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) {
                currentTab.getCodeArea().requestFocus();
            }
        });

        // Set the window title
        stage.setTitle("Tricky Teta"); // Updated name to reflect purpose
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Set up a listener to focus the editor when a tab is selected
     */
    private void setupTabSelectionHandler() {
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                EditorTab editorTab = tabContents.get(newTab);
                if (editorTab != null) {
                    // Request focus on the code area when tab is selected
                    Platform.runLater(() -> editorTab.getCodeArea().requestFocus());

                    // Update status bar with current position
                    updateStatusBar(editorTab.getCodeArea());
                }
            }
        });
    }

    /**
     * Updates the status bar with current cursor position
     */
    private void updateStatusBar(CodeArea codeArea) {
        int line = codeArea.getCurrentParagraph() + 1;
        int column = codeArea.getCaretColumn() + 1;
        statusLabel.setText("Line: " + line + ", Column: " + column);
    }

    /**
     * Apply custom CSS styles to the scene
     */
    private void applyCustomStyles(Scene scene) {
        // Basic scrollbar styling for the CodeArea
        String customStyles =
                // Basic scrollbar styling for the CodeArea
                ".virtual-flow .scroll-bar:vertical {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "    -fx-opacity: 1.0;" +
                        "    -fx-pref-width: 12px;" +
                        "}" +
                        ".virtual-flow .scroll-bar:vertical .thumb {" +
                        "    -fx-background-color: " + SCROLLBAR_THUMB_COLOR + ";" +
                        "    -fx-background-radius: 6px;" +
                        "    -fx-opacity: 0.8;" +
                        "}" +
                        ".virtual-flow .scroll-bar:vertical .thumb:hover {" +
                        "    -fx-background-color: #888888;" +
                        "    -fx-opacity: 1.0;" +
                        "}" +
                        ".virtual-flow .scroll-bar:vertical .track {" +
                        "    -fx-background-color: " + SCROLLBAR_TRACK_COLOR + ";" +
                        "    -fx-background-radius: 0;" +
                        "}" +
                        // Hide increment and decrement buttons for a cleaner look
                        ".virtual-flow .scroll-bar:vertical .increment-button," +
                        ".virtual-flow .scroll-bar:vertical .decrement-button {" +
                        "    -fx-opacity: 0;" +
                        "    -fx-pref-height: 0;" +
                        "    -fx-min-height: 0;" +
                        "    -fx-max-height: 0;" +
                        "}" +
                        // Hide the arrow icons
                        ".virtual-flow .scroll-bar:vertical .increment-arrow," +
                        ".virtual-flow .scroll-bar:vertical .decrement-arrow {" +
                        "    -fx-opacity: 0;" +
                        "    -fx-pref-height: 0;" +
                        "    -fx-min-height: 0;" +
                        "    -fx-max-height: 0;" +
                        "}" +
                        // Line numbers styling
                        ".lineno {" +
                        "    -fx-background-color: #3D3D3D;" +
                        "    -fx-text-fill: #888888;" +
                        "}" +
                        // Selection styling
                        ".styled-text-area .selection {" +
                        "    -fx-fill: #214283;" +
                        "}" +
                        // Base colors for the code area
                        ".styled-text-area {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
                        ".styled-text-area .text {" +
                        "    -fx-fill: " + TEXT_COLOR + ";" +
                        "}" +
                        // Enhanced caret styling for better visibility
                        ".styled-text-area .caret {" +
                        "    -fx-stroke-width: 2.0;" +
                        "    -fx-stroke: " + CURSOR_COLOR + ";" +
                        "}" +
                        // Current line highlighting
                        ".styled-text-area .current-line {" +
                        "    -fx-background-color: " + CURRENT_LINE_COLOR + ";" +
                        "}";

        // Custom tab styling
        String customTabStyle =
                ".tab-pane .tab-header-area .tab-header-background {" +
                        "    -fx-background-color: " + TAB_HEADER_COLOR + ";" +
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
                        "    -fx-border-color: " + CURSOR_COLOR + ";" + // Orange indicator for selected tab
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

        // Add both styles to the scene
        scene.getStylesheets().add("data:text/css," + customStyles.replace(" ", "%20"));
        scene.getStylesheets().add("data:text/css," + customTabStyle.replace(" ", "%20"));
    }

    /**
     * Inner class to represent the content of a tab
     */
    private class EditorTab {
        private CodeArea codeArea;
        private VirtualizedScrollPane<CodeArea> scrollPane;
        private String fileName;
        private File file;

        public EditorTab(String fileName) {
            this.fileName = fileName;
            this.file = null;

            // Create the code area
            codeArea = new CodeArea();

            // Add line numbers
            codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

            // Make sure the code area is editable and can receive focus/clicks
            codeArea.setEditable(true);
            codeArea.setMouseTransparent(false);
            codeArea.setFocusTraversable(true);

            // Add explicit mouse click handler to ensure focus
            codeArea.setOnMouseClicked(event -> {
                codeArea.requestFocus();
            });

            // Apply styling
            codeArea.setStyle(
                    "-fx-background-color: " + BACKGROUND_COLOR + ";" +
                            "-fx-font-family: 'Monospace';" +
                            "-fx-font-size: 14px;" +
                            "-fx-text-fill: " + TEXT_COLOR + ";"
            );

            // Enable word wrap for note-taking
            codeArea.setWrapText(true);

            // Setup current line highlighting
            codeArea.currentParagraphProperty().addListener((obs, oldLine, newLine) -> {
                // Remove highlight from old line
                if (oldLine != null) {
                    codeArea.setStyle(oldLine.intValue(), Collections.emptyList());
                }

                // Add highlight to new line
                if (newLine != null) {
                    codeArea.setStyle(newLine.intValue(), Collections.singleton("current-line"));
                }
            });

            // Update status bar when caret position changes
            codeArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
                updateStatusBar(codeArea);
            });

            // Setup caret flash time for better visibility (optional enhancement)
            codeArea.setStyle("-fx-caret-blink-rate: 500ms;");

            // Create a virtualized scroll pane that properly handles scrolling
            scrollPane = new VirtualizedScrollPane<>(codeArea);
        }

        public VirtualizedScrollPane<CodeArea> getScrollPane() {
            return scrollPane;
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
        tab.setContent(editorTab.getScrollPane()); // Use the scroll pane instead of direct CodeArea

        // Store tab data
        tabContents.put(tab, editorTab);

        // Add tab to pane
        tabPane.getTabs().add(tab);

        // Select the new tab
        tabPane.getSelectionModel().select(tab);

        // Focus the editor in the new tab
        Platform.runLater(() -> editorTab.getCodeArea().requestFocus());

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

        // Add filters for text files only
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
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

            // Move cursor to beginning of file
            editorTab.getCodeArea().moveTo(0);

            // Make sure to focus the code area
            Platform.runLater(() -> editorTab.getCodeArea().requestFocus());
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

            // Refocus the code area after saving
            Platform.runLater(() -> editorTab.getCodeArea().requestFocus());
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
            if (currentTab != null) {
                currentTab.getCodeArea().cut();
                // Ensure focus after operation
                Platform.runLater(() -> currentTab.getCodeArea().requestFocus());
            }
        });

        scene.getAccelerators().put(keyCombCopy, () -> {
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) {
                currentTab.getCodeArea().copy();
                // Ensure focus after operation
                Platform.runLater(() -> currentTab.getCodeArea().requestFocus());
            }
        });

        scene.getAccelerators().put(keyCombPaste, () -> {
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) {
                currentTab.getCodeArea().paste();
                // Ensure focus after operation
                Platform.runLater(() -> currentTab.getCodeArea().requestFocus());
            }
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