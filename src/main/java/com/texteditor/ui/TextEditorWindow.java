package com.texteditor.ui;

import com.texteditor.io.FileHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.flowless.VirtualizedScrollPane;

import java.io.File;
import java.util.*;

import static javafx.application.Application.STYLESHEET_MODENA;
import static javafx.application.Application.setUserAgentStylesheet;
import static org.fxmisc.richtext.model.TwoDimensional.Bias.Forward;

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

    private TabPane tabPane;// TabPane to hold multiple document tabs
    private BorderPane root;
    private TextField searchField;
    private HBox findBar;
    private int untitledCount = 1;  // Counter for untitled documents
    private Map<Tab, EditorTab> tabContents = new HashMap<>();  // Map to store tab data
    private Label statusLabel;// Status bar to display cursor position
    private final List<String> clipboardHistory = new ArrayList<>();


    /**
     * Initializes and displays the text editor window
     * @param stage The primary stage (window) for the application
     */
    public void start(Stage stage) {
        // Set up platform configuration

        // App exits automatically when last window closes
        Platform.setImplicitExit(true);

        // Apply modena
        setUserAgentStylesheet(STYLESHEET_MODENA);

        //Reset it to rely on custom styling
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
        root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(statusLabel);

        // Create a scene with the root pane
        StackPane stack = new StackPane();
        stack.getChildren().add(root);
        Scene scene = new Scene(stack, 800, 600);

        initializeFindBar(stack, stage);

        // Set up keyboard shortcuts
        setupKeyboardShortcuts(scene, stage);

        // Apply CSS styles with custom scrollbar styling
        applyCustomStyles(scene);

        // Create initial tabs with empty content
        createNewTab("Untitled");

        // Select the first tab
        tabPane.getSelectionModel().select(0);

        // Focus the code area in the first tab
        // Text area becomes active and ready for input
        Platform.runLater(() -> {
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) {
                currentTab.getCodeArea().requestFocus();
            }
        });

        // Set up tab close request handler
        setupTabCloseHandler();

        // Set up window close handler
        setupWindowCloseHandler(stage);

        // Set the window title
        stage.setTitle("Tricky Teta");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Set up a handler for tab close events
     */
    private void setupTabCloseHandler() {
        // Add a custom event handler for tab close requests
        tabPane.setTabDragPolicy(TabPane.TabDragPolicy.REORDER);

        // Add listener to detect tab close button clicks
        tabPane.getTabs().forEach(tab -> {
            tab.setOnCloseRequest(event -> {
                // Check if the tab content has unsaved changes
                EditorTab editorTab = tabContents.get(tab);
                if (editorTab != null && editorTab.hasUnsavedChanges()) {
                    // Show confirmation dialog
                    boolean shouldClose = showUnsavedChangesDialog(tab.getText());
                    if (!shouldClose) {
                        // Cancel the close event
                        event.consume();
                    }
                }
            });
        });
    }

    /**
     * Set up a handler for window close events
     */
    private void setupWindowCloseHandler(Stage stage) {
        stage.setOnCloseRequest(event -> {
            // Check if any tab has unsaved changes
            boolean hasUnsavedChanges = false;
            for (EditorTab tab : tabContents.values()) {
                if (tab.hasUnsavedChanges()) {
                    hasUnsavedChanges = true;
                    break;
                }
            }

            if (hasUnsavedChanges) {
                // Show confirmation dialog
                boolean shouldClose = showUnsavedChangesDialog("Tricky Teta");
                if (!shouldClose) {
                    // Cancel the close event
                    event.consume();
                }
            }
        });
    }

    /**
     * Show dialog for unsaved changes
     * @return true if the operation should proceed, false if canceled
     */
    private boolean showUnsavedChangesDialog(String tabName) {
        // Create alert dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Unsaved changes in " + tabName);
        alert.setContentText("Would you like to save changes before closing?");

        // Customize buttons
        ButtonType saveButton = new ButtonType("Save");
        ButtonType dontSaveButton = new ButtonType("Don't Save");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(saveButton, dontSaveButton, cancelButton);

        // Apply dark theme to the dialog
        applyDarkThemeToDialog(alert);

        // Show dialog and process response
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == saveButton) {
                // Save the file
                saveCurrentTab((Stage) alert.getOwner());
                return true;
            } else if (result.get() == dontSaveButton) {
                // Close without saving
                return true;
            } else {
                // Cancel the close operation
                return false;
            }
        }
        return false;
    }

    /**
     * Apply dark theme to dialog
     */
    private void applyDarkThemeToDialog(Dialog<?> dialog) {
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle(
                "-fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "-fx-text-fill: " + TEXT_COLOR + ";"
        );

        // Apply style to buttons
        dialogPane.getButtonTypes().forEach(buttonType -> {
            Button button = (Button) dialogPane.lookupButton(buttonType);
            button.setStyle(
                    "-fx-background-color: #444444;" +
                            "-fx-text-fill: " + TEXT_COLOR + ";" +
                            "-fx-border-color: #666666;" +
                            "-fx-border-radius: 3px;"
            );
        });

        // Add CSS to handle label text color
        dialog.getDialogPane().getScene().getRoot().setStyle(
                "-fx-text-fill: " + TEXT_COLOR + ";" +
                        "-fx-font-size: 14px;"
        );

        // Set text color for header and content text
        Label headerLabel = (Label) dialogPane.lookup(".header-panel .label");
        if (headerLabel != null) {
            headerLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
        }

        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: " + TEXT_COLOR + ";");
        }
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
                        "}" +
                        // Fix for white scroll pane background
                        ".scroll-pane, .scroll-pane > .viewport {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
                        // Fix for virtualized scroll pane (RichTextFX specific)
                        ".virtualized-scroll-pane {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
                        // Fix for horizontal scrollbar
                        ".virtual-flow .scroll-bar:horizontal," +
                        ".scroll-bar:horizontal {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "    -fx-opacity: 1.0;" +
                        "}" +
                        ".virtual-flow .scroll-bar:horizontal .thumb," +
                        ".scroll-bar:horizontal .thumb {" +
                        "    -fx-background-color: " + SCROLLBAR_THUMB_COLOR + ";" +
                        "    -fx-background-radius: 6px;" +
                        "    -fx-opacity: 0.8;" +
                        "}" +
                        ".virtual-flow .scroll-bar:horizontal .track," +
                        ".scroll-bar:horizontal .track {" +
                        "    -fx-background-color: " + SCROLLBAR_TRACK_COLOR + ";" +
                        "    -fx-background-radius: 0;" +
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
                        "}" +
                        // Fix for tab content area
                        ".tab-pane > .tab-content-area {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}";

        // Root style to ensure all parts of the application use dark theme
        String rootStyle =
                // Style for the root BorderPane and Scene
                ".root {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "    -fx-base: " + BACKGROUND_COLOR + ";" +
                        "}";

        // Add dialog styling
        String dialogStyle =
                // Style for dialog boxes
                ".dialog-pane {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
                        ".dialog-pane .header-panel {" +
                        "    -fx-background-color: #3D3D3D;" +
                        "}" +
                        ".dialog-pane .header-panel .label {" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "}" +
                        ".dialog-pane .content.label {" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "}" +
                        ".dialog-pane .button {" +
                        "    -fx-background-color: #444444;" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "    -fx-border-color: #666666;" +
                        "    -fx-border-radius: 3px;" +
                        "}" +
                        ".dialog-pane .button:hover {" +
                        "    -fx-background-color: #555555;" +
                        "}" +
                        ".dialog-pane:header .header-panel .label {" +
                        "    -fx-font-size: 16px;" +
                        "    -fx-font-weight: bold;" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "}";

        // Add all styles to the scene
        scene.getStylesheets().add("data:text/css," + customStyles.replace(" ", "%20"));
        scene.getStylesheets().add("data:text/css," + customTabStyle.replace(" ", "%20"));
        scene.getStylesheets().add("data:text/css," + rootStyle.replace(" ", "%20"));
        scene.getStylesheets().add("data:text/css," + dialogStyle.replace(" ", "%20"));
    }

    /**
     * Inner class to represent the content of a tab
     */
    private class EditorTab {
        private CodeArea codeArea;
        private VirtualizedScrollPane<CodeArea> scrollPane;
        private String fileName;
        private File file;
        private String lastSavedContent;
        private boolean isModified = false;

        public EditorTab(String fileName) {
            this.fileName = fileName;
            this.file = null;

            // Create the code area
            codeArea = new CodeArea();
            lastSavedContent = "";

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

            // Track text changes for "unsaved changes" detection
            codeArea.textProperty().addListener((obs, oldText, newText) -> {
                if (!newText.equals(lastSavedContent)) {
                    isModified = true;
                } else {
                    isModified = false;
                }
            });

            // Add a key event filter to the code area to listen for key presses
            codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {

                // Check if the user pressed Ctrl+C (copy)
                if (event.isControlDown() && event.getCode() == KeyCode.C) {

                    // Get the currently selected text in the code area
                    String selectedText = codeArea.getSelectedText();

                    // Only proceed if there is something selected
                    if (!selectedText.isEmpty()) {

                        // Add the selected text to the beginning of the clipboard history list
                        clipboardHistory.add(0, selectedText);

                        // Limit the clipboard history size to 20 items (FIFO)
                        if (clipboardHistory.size() > 20) {
                            clipboardHistory.remove(clipboardHistory.size() - 1); // Remove the oldest entry
                        }
                    }
                }
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

        public boolean hasUnsavedChanges() {
            return isModified;
        }

        public void markAsSaved() {
            lastSavedContent = codeArea.getText();
            isModified = false;
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

        // Add close handler to remove from map and check for unsaved changes
        tab.setOnClosed(e -> tabContents.remove(tab));

        // Add close request handler to this tab
        tab.setOnCloseRequest(event -> {
            EditorTab content = tabContents.get(tab);
            if (content != null && content.hasUnsavedChanges()) {
                boolean shouldClose = showUnsavedChangesDialog(tab.getText());
                if (!shouldClose) {
                    event.consume();
                }
            }
        });

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
        FileChooser.ExtensionFilter allFilesFilter = new FileChooser.ExtensionFilter("All Files", "*", "*.*");
        fileChooser.getExtensionFilters().addAll(
                allFilesFilter,
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        // Set "All Files" as the default filter
        fileChooser.setSelectedExtensionFilter(allFilesFilter);

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
            editorTab.markAsSaved();

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

            // Mark as saved
            editorTab.markAsSaved();

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
        KeyCombination keyCombClipboard = new KeyCodeCombination(KeyCode.V,KeyCombination.CONTROL_DOWN,KeyCombination.SHIFT_DOWN);

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
            if (currentTab != null) {
                // Check for unsaved changes before closing
                EditorTab editorTab = tabContents.get(currentTab);
                if (editorTab != null && editorTab.hasUnsavedChanges()) {
                    boolean shouldClose = showUnsavedChangesDialog(currentTab.getText());
                    if (shouldClose) {
                        tabPane.getTabs().remove(currentTab);
                    }
                } else {
                    tabPane.getTabs().remove(currentTab);
                }
            }
        });

        // Exit application (Ctrl+Q)
        scene.getAccelerators().put(keyCombExit, () -> {
            // Create a window close request event
            WindowEvent closeEvent = new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST);
            // Send the event to the stage
            stage.fireEvent(closeEvent);
            // If the event was not consumed (i.e., closing wasn't cancelled)
            if (!closeEvent.isConsumed()) {
                stage.close();
            }
        });

        // Add keyboard handler for Ctrl+Tab to switch tabs
        KeyCombination keyCombNextTab = new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (keyCombNextTab.match(event)) {
                int currentIndex = tabPane.getSelectionModel().getSelectedIndex();
                int tabCount = tabPane.getTabs().size();
                if (tabCount > 1) {
                    int nextIndex = (currentIndex + 1) % tabCount;
                    tabPane.getSelectionModel().select(nextIndex);
                }
                event.consume();
            }
        });

        // Add keyboard handler for Ctrl+Shift+Tab to switch tabs in reverse
        KeyCombination keyCombPrevTab = new KeyCodeCombination(KeyCode.TAB, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (keyCombPrevTab.match(event)) {
                int currentIndex = tabPane.getSelectionModel().getSelectedIndex();
                int tabCount = tabPane.getTabs().size();
                if (tabCount > 1) {
                    int prevIndex = (currentIndex - 1 + tabCount) % tabCount;
                    tabPane.getSelectionModel().select(prevIndex);
                }
                event.consume();
            }
        });

        // Add Ctrl+F for find functionality
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> {
            findBar.setVisible(true);
            Platform.runLater(() -> {
                searchField.requestFocus();
                searchField.selectAll();
            });
        });

        // Add handler to show clipboard history
        scene.getAccelerators().put(keyCombClipboard, () -> {
            showClipboardPopup(); // This should be a method that displays your clipboard history
        });


    }

    private void showClipboardPopup() {
        if (clipboardHistory.isEmpty()) return;

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(clipboardHistory);

        // Dark theme & remove focus/borders
        listView.setStyle(
                "-fx-control-inner-background: #2b2b2b;" +
                        "-fx-background-color: #2b2b2b;" +
                        "-fx-border-color: transparent;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;" +
                        "-fx-selection-bar: #444;" +
                        "-fx-selection-bar-text: #ddd;" +
                        "-fx-highlight-fill: transparent;" +
                        "-fx-highlight-text-fill: #ddd;"
        );

        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    EditorTab currentTab = getCurrentEditorTab();
                    if (currentTab != null) {
                        CodeArea codeArea = currentTab.getCodeArea();
                        int caretPos = codeArea.getCaretPosition();
                        codeArea.insertText(caretPos, selected);
                    }
                    ((Stage) listView.getScene().getWindow()).close();
                }
            }
        });

        VBox container = new VBox(listView);
        container.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 10;");

        Scene scene = new Scene(container, 400, 300);
        scene.setFill(Color.web("#2b2b2b"));

        Stage popupStage = new Stage(StageStyle.DECORATED);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Clipboard History");
        popupStage.setScene(scene);
        popupStage.showAndWait();
    }





    /**
     * Shows a dialog for finding text in the current document
     */
    private void initializeFindBar(StackPane stack, Stage stage) {
        searchField = new TextField();
        searchField.setPromptText("Find...");
        searchField.setPrefWidth(200);  // Limit width
        searchField.setStyle(
                "-fx-background-color: #222; -fx-text-fill: #DDD; -fx-prompt-text-fill: #888; " +
                        "-fx-border-color: #444; -fx-border-radius: 4px; -fx-background-radius: 4px;"
        );

        Button closeBtn = new Button("✕");
        closeBtn.setOnAction(e -> findBar.setVisible(false));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #CCC;");

        Button nextBtn = new Button("↓");
        nextBtn.setStyle("-fx-background-color: #333; -fx-text-fill: #CCC; -fx-background-radius: 3;");

        Button prevBtn = new Button("↑");
        prevBtn.setStyle("-fx-background-color: #333; -fx-text-fill: #CCC; -fx-background-radius: 3;");

        // Setup search navigation buttons
        nextBtn.setOnAction(e -> findNext(searchField.getText()));
        prevBtn.setOnAction(e -> findPrevious(searchField.getText()));

        findBar = new HBox(5, searchField, prevBtn, nextBtn, closeBtn);
        findBar.setPadding(new Insets(5));
        findBar.setMaxHeight(35);  // Constrain height
        findBar.setMaxWidth(Region.USE_PREF_SIZE);  // Use preferred size for width
        findBar.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #555; " +
                "-fx-border-radius: 3; -fx-background-radius: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 2);");
        findBar.setAlignment(Pos.CENTER_LEFT);
        findBar.setVisible(false); // Initially hidden

        // Position in top-right with proper margins
        StackPane.setAlignment(findBar, Pos.TOP_RIGHT);
        StackPane.setMargin(findBar, new Insets(10, 15, 0, 0));

        // Add to stack pane on top of the content
        stack.getChildren().add(findBar);

        // Ensure the find bar is on top
        findBar.toFront();

        // Handle search logic on Enter key
        searchField.setOnAction(e -> findNext(searchField.getText()));

        // Add keyboard handler to close find bar on ESC
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                findBar.setVisible(false);
                // Return focus to editor
                EditorTab currentTab = getCurrentEditorTab();
                if (currentTab != null) {
                    currentTab.getCodeArea().requestFocus();
                }
                event.consume();
            }
        });
    }

    // Helper method for finding the next occurrence
    private void findNext(String query) {
        if (query == null || query.isEmpty()) return;

        CodeArea codeArea = getCurrentEditorTab().getCodeArea();
        String text = codeArea.getText();
        int start = codeArea.getSelection().getEnd();
        int index = text.indexOf(query, start);

        // If not found from current position, wrap around to beginning
        if (index == -1 && start > 0) {
            index = text.indexOf(query);
        }

        if (index != -1) {
            // Move caret to the found word
            int paragraph = codeArea.offsetToPosition(index, Forward).getMajor();
            int column = codeArea.offsetToPosition(index, Forward).getMinor();
            codeArea.moveTo(paragraph, column);
            codeArea.requestFollowCaret();

            // Select the word after moving
            codeArea.selectRange(index, index + query.length());

            searchField.setStyle(
                    "-fx-background-color: #222; -fx-text-fill: #DDD; -fx-prompt-text-fill: #888; " +
                            "-fx-border-color: #444; -fx-border-radius: 4px; -fx-background-radius: 4px;"
            );
        }

        else {
            // Visual feedback for not found
            searchField.setStyle("-fx-background-color: #400; -fx-text-fill: #DDD;");
        }
        // Return focus to the search field for continued searching
        searchField.requestFocus();
    }

    private void findPrevious(String query) {
        if (query == null || query.isEmpty()) return;

        CodeArea codeArea = getCurrentEditorTab().getCodeArea();
        String text = codeArea.getText();
        int start = codeArea.getSelection().getStart() - 1;

        if (start < 0) start = 0;

        int index = text.lastIndexOf(query, start);

        if (index == -1) {
            index = text.lastIndexOf(query);
        }

        if (index != -1) {
            int paragraph = codeArea.offsetToPosition(index, Forward).getMajor();
            int column = codeArea.offsetToPosition(index, Forward).getMinor();
            codeArea.moveTo(paragraph, column);
            codeArea.requestFollowCaret();

            codeArea.selectRange(index, index + query.length());

            searchField.setStyle(
                    "-fx-background-color: #222; -fx-text-fill: #DDD; -fx-prompt-text-fill: #888; " +
                            "-fx-border-color: #444; -fx-border-radius: 4px; -fx-background-radius: 4px;"
            );
        }
        else {
            searchField.setStyle("-fx-background-color: #400; -fx-text-fill: #DDD;");
        }

        searchField.requestFocus();
    }




}