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
import java.util.regex.Pattern;

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

    // Replace your existing color constants with these:
    private static final String BACKGROUND_COLOR = "#1E1E2E";         // Dark purple background
    private static final String TEXT_COLOR = "#E0E0E0";               // Soft white text
    private static final String TAB_SELECTED_COLOR = "#2A2A3A";       // Slightly lighter purple
    private static final String TAB_UNSELECTED_COLOR = "#252535";     // Dark purple for tabs
    private static final String TAB_HEADER_COLOR = "#1E1E2E";         // Dark purple header
    private static final String SCROLLBAR_THUMB_COLOR = "#6D5D8C";    // Purple-grey scrollbar
    private static final String SCROLLBAR_TRACK_COLOR = "#2A2A3A";    // Dark purple track
    private static final String CURSOR_COLOR = "#BB86FC";             // Light purple cursor
    private static final String CURRENT_LINE_COLOR = "#2A2A3A";       // Current line highlight

    // Purple-themed accent colors
    private static final String MAIN_HEADER_COLOR = "#BB86FC";        // Light purple headers
    private static final String SUB_ITEM_COLOR = "#03DAC6";           // Teal for sub-items
    private static final String BULLET_COLOR = "#888888";             // Grey bullets
    private static final String IMPORTANT_COLOR = "#FF79C6";          // Pink emphasis
    private static final String HIGHLIGHT_COLOR = "#6D3D8C";          // Purple selection
    private static final String FIND_BAR_COLOR = "#2A2A3A";           // Find bar color

    // Regex patterns for auto-styling
    private static final Pattern MAIN_HEADER_PATTERN = Pattern.compile("^.+:$");  // Lines ending with :
    private static final Pattern SUB_ITEM_PATTERN = Pattern.compile("^\\s*[a-z]\\).*");  // Lines starting with a), b), etc.
    private static final Pattern BULLET_PATTERN = Pattern.compile("^\\s*-.*");  // Lines starting with -
    private static final Pattern DASH_HEADER_PATTERN = Pattern.compile("^[A-Z][a-zA-Z\\s&]+$");  // All caps headers like "Smart Contract Lifecycle"

    private TabPane tabPane;// TabPane to hold multiple document tabs
    private TextField searchField;
    private HBox findBar;
    private int untitledCount = 1;// Counter for untitled documents
    private int currentFontSize = 14; // Default font size
    private final Map<Tab, EditorTab> tabContents = new HashMap<>();  // Map to store tab data
    private Label statusLabel;// Status bar to display cursor position
    private Timer fontSizeDisplayTimer;
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
        statusLabel.setStyle(
                "-fx-background-color: #2A2A3A;" +
                        "-fx-text-fill: #BBBBBB;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 5px 15px;" +
                        "-fx-border-width: 1px 0 0 0;" +
                        "-fx-border-color: #3D3D4D;"
        );
        // Create the root layout container
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        root.setBottom(statusLabel);

        // Create a scene with the root pane
        StackPane stack = new StackPane();
        stack.getChildren().add(root);
        Scene scene = new Scene(stack, 800, 600);

        initializeFindBar(stack);

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
        tabPane.getTabs().forEach(tab -> tab.setOnCloseRequest(event -> {
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
        }));
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
            // Close without saving
            // Cancel the close operation
            if (result.get() == saveButton) {
                // Save the file
                saveCurrentTab((Stage) alert.getOwner());
                return true;
            } else return result.get() == dontSaveButton;
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
     * Apply automatic styling based on note formatting patterns
     */
    private void applyAutoStyling(CodeArea codeArea) {
        String text = codeArea.getText();
        String[] lines = text.split("\n", -1);

        // Clear all existing styles first
        codeArea.clearStyle(0, text.length());

        int currentIndex = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmedLine = line.trim();

            if (!trimmedLine.isEmpty()) {
                // Style collection for this line
                Collection<String> styles = new ArrayList<>();

                // Main headers (lines ending with :)
                if (MAIN_HEADER_PATTERN.matcher(trimmedLine).matches()) {
                    styles.add("main-header");
                }
                // Sub-items (a), b), c), etc.)
                else if (SUB_ITEM_PATTERN.matcher(line).matches()) {
                    styles.add("sub-item");
                }

                // Standalone headers (like "Smart Contract Lifecycle")
                else if (DASH_HEADER_PATTERN.matcher(trimmedLine).matches() && !trimmedLine.contains(":")) {
                    styles.add("section-header");
                }

                // Apply styles to the entire line range
                if (!styles.isEmpty()) {
                    int lineStart = currentIndex;
                    int lineEnd = currentIndex + line.length();
                    codeArea.setStyle(lineStart, lineEnd, styles);
                }
            }

            // Move to next line (including newline character)
            currentIndex += line.length() + 1;
        }
    }


    private void applyCustomStyles(Scene scene) {
        // Basic scrollbar and editor styling
        String customStyles =
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
                        ".virtual-flow .scroll-bar:vertical .increment-button," +
                        ".virtual-flow .scroll-bar:vertical .decrement-button {" +
                        "    -fx-opacity: 0;" +
                        "    -fx-pref-height: 0;" +
                        "    -fx-min-height: 0;" +
                        "    -fx-max-height: 0;" +
                        "}" +
                        ".virtual-flow .scroll-bar:vertical .increment-arrow," +
                        ".virtual-flow .scroll-bar:vertical .decrement-arrow {" +
                        "    -fx-opacity: 0;" +
                        "    -fx-pref-height: 0;" +
                        "    -fx-min-height: 0;" +
                        "    -fx-max-height: 0;" +
                        "}" +
                        ".lineno {" +
                        "    -fx-background-color: #3D3D4D;" +
                        "    -fx-text-fill: #888888;" +
                        "}" +
                        ".styled-text-area .selection {" +
                        "    -fx-fill: " + HIGHLIGHT_COLOR + ";" +
                        "}" +
                        ".styled-text-area {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
                        ".styled-text-area .text {" +
                        "    -fx-fill: " + TEXT_COLOR + ";" +
                        "}" +
                        ".styled-text-area .caret {" +
                        "    -fx-stroke-width: 2.0;" +
                        "    -fx-stroke: " + CURSOR_COLOR + ";" +
                        "}" +
                        ".styled-text-area .current-line {" +
                        "    -fx-background-color: " + CURRENT_LINE_COLOR + ";" +
                        "}" +
                        ".scroll-pane, .scroll-pane > .viewport {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
                        ".virtualized-scroll-pane {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
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
                        "}" +
                        ".styled-text-area .main-header {" +
                        "    -fx-fill: " + MAIN_HEADER_COLOR + ";" +
                        "    -fx-font-weight: bold;" +
                        "    -fx-font-size: " + (currentFontSize + 4) + "px;" +
                        "}" +
                        ".styled-text-area .sub-item {" +
                        "    -fx-fill: " + SUB_ITEM_COLOR + ";" +
                        "    -fx-font-weight: bold;" +
                        "}" +
                        ".styled-text-area .bullet-point {" +
                        "    -fx-fill: " + BULLET_COLOR + ";" +
                        "}" +
                        ".styled-text-area .section-header {" +
                        "    -fx-fill: " + IMPORTANT_COLOR + ";" +
                        "    -fx-font-weight: bold;" +
                        "    -fx-font-size: " + (currentFontSize + 2) + "px;" +
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
                        ".tab-pane .tab {" +
                        "    -fx-focus-color: transparent;" +
                        "    -fx-faint-focus-color: transparent;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab {" +
                        "    -fx-background-color: " + TAB_UNSELECTED_COLOR + ";" +
                        "    -fx-background-radius: 0;" +
                        "    -fx-padding: 8px 15px;" +
                        "    -fx-pref-width: 200px;" +
                        "    -fx-max-width: 200px;" +
                        "    -fx-min-width: 150px;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected {" +
                        "    -fx-background-color: " + TAB_SELECTED_COLOR + ";" +
                        "    -fx-border-width: 0 0 2px 0;" +
                        "    -fx-border-color: " + CURSOR_COLOR + ";" +
                        "    -fx-focus-color: transparent;" +
                        "    -fx-faint-focus-color: transparent;" +
                        "}" +
                        ".tab-pane .tab .tab-label {" +
                        "    -fx-text-fill: #CCCCCC;" +
                        "    -fx-font-size: 13px;" +
                        "}" +
                        ".tab-pane .tab:selected .tab-label {" +
                        "    -fx-text-fill: #FFFFFF;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab > .tab-container > .tab-close-button {" +
                        "    -fx-background-color: #CCCCCC;" +
                        "    -fx-shape: \"M 0,0 H1 L 4,3 7,0 H8 V1 L 5,4 8,7 V8 H7 L 4,5 1,8 H0 V7 L 3,4 0,1 Z\";" +
                        "    -fx-scale-shape: false;" +
                        "}" +
                        ".tab-pane > .tab-content-area {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}";

        // Root style to ensure all parts of the application use dark theme
        String rootStyle =
                ".root {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "    -fx-base: " + BACKGROUND_COLOR + ";" +
                        "}";

        // Add dialog styling
        String dialogStyle =
                ".dialog-pane {" +
                        "    -fx-background-color: " + BACKGROUND_COLOR + ";" +
                        "}" +
                        ".dialog-pane .header-panel {" +
                        "    -fx-background-color: #3D3D4D;" +
                        "}" +
                        ".dialog-pane .header-panel .label {" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "}" +
                        ".dialog-pane .content.label {" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "}" +
                        ".dialog-pane .button {" +
                        "    -fx-background-color: #444455;" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "    -fx-border-color: #666677;" +
                        "    -fx-border-radius: 3px;" +
                        "}" +
                        ".dialog-pane .button:hover {" +
                        "    -fx-background-color: #555566;" +
                        "}" +
                        ".dialog-pane:header .header-panel .label {" +
                        "    -fx-font-size: 16px;" +
                        "    -fx-font-weight: bold;" +
                        "    -fx-text-fill: " + TEXT_COLOR + ";" +
                        "}";

        // Add all styles to the scene
        scene.getStylesheets().add("data:text/css," + encodeCSS(customStyles));
        scene.getStylesheets().add("data:text/css," + encodeCSS(customTabStyle));
        scene.getStylesheets().add("data:text/css," + encodeCSS(rootStyle));
        scene.getStylesheets().add("data:text/css," + encodeCSS(dialogStyle));
    }
    /**
     * Helper method to properly encode CSS for data URLs
     */
    private String encodeCSS(String css) {
        return css.replace(" ", "%20")
                .replace("\n", "%0A")
                .replace("\r", "%0D")
                .replace("\"", "%22")
                .replace("#", "%23")
                .replace("&", "%26")
                .replace("'", "%27")
                .replace("(", "%28")
                .replace(")", "%29")
                .replace("+", "%2B")
                .replace(",", "%2C")
                .replace("/", "%2F")
                .replace(":", "%3A")
                .replace(";", "%3B")
                .replace("=", "%3D")
                .replace("?", "%3F")
                .replace("@", "%40")
                .replace("[", "%5B")
                .replace("]", "%5D");
    }

    /**
     * Inner class to represent the content of a tab
     */
    private class EditorTab {
        private final CodeArea codeArea;
        private final VirtualizedScrollPane<CodeArea> scrollPane;
        private File file;
        private String lastSavedContent;
        private boolean isModified = false;

        public EditorTab() {
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
            codeArea.setOnMouseClicked(event -> codeArea.requestFocus());

            // Update the code area styling:
            codeArea.setStyle(
                    "-fx-background-color: " + BACKGROUND_COLOR + ";" +
                            "-fx-font-family: 'JetBrains Mono', 'Fira Code', monospace;" +
                            "-fx-font-size: " + currentFontSize + "px;" +
                            "-fx-text-fill: " + TEXT_COLOR + ";" +
                            "-fx-font-smoothing-type: lcd;" +
                            "-fx-padding: 10px;"
            );

            // Enable word wrap for note-taking
            codeArea.setWrapText(true);

            // Setup current line highlighting
            codeArea.currentParagraphProperty().addListener((obs, oldLine, newLine) -> {
                // Remove highlight from old line
                if (oldLine != null) {
                    codeArea.setStyle(oldLine, Collections.emptyList());
                }

                // Add highlight to new line
                if (newLine != null) {
                    codeArea.setStyle(newLine, Collections.singleton("current-line"));
                }
            });

            // Update status bar when caret position changes
            codeArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> updateStatusBar(codeArea));

            // Track text changes for "unsaved changes" detection
            codeArea.textProperty().addListener((obs, oldText, newText) -> isModified = !newText.equals(lastSavedContent));

            // Add a key event filter to the code area to listen for key presses
            codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                // Check if the user pressed Ctrl+C (copy)
                if (event.isControlDown() && event.getCode() == KeyCode.C) {
                    // Get the currently selected text in the code area
                    String selectedText = codeArea.getSelectedText();

                    // Only proceed if there is something selected
                    if (!selectedText.isEmpty()) {
                        // Add the selected text to the beginning of the clipboard history list
                        clipboardHistory.addFirst(selectedText);

                        // Limit the clipboard history size to 20 items (FIFO)
                        if (clipboardHistory.size() > 20) {
                            clipboardHistory.removeLast(); // Remove the oldest entry
                        }
                    }
                }
            });

            // Add auto-styling when text changes
            codeArea.textProperty().addListener((obs, oldText, newText) -> {
                isModified = !newText.equals(lastSavedContent);
                // Apply auto-styling with a small delay to avoid performance issues
                Platform.runLater(() -> applyAutoStyling(codeArea));
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

        public void setFileName() {
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
    private void createNewTab() {
        String defaultName = "Untitled " + untitledCount++;
        createNewTab(defaultName);
    }

    /**
     * Creates a new tab with the specified filename
     */
    private Tab createNewTab(String fileName) {
        // Create new editor content
        EditorTab editorTab = new EditorTab();

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
            editorTab.setFileName();
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
                    editorTab.setFileName();
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

    private void setFontSize(int fontSize) {
        EditorTab currentTab = getCurrentEditorTab();
        if (currentTab != null) {
            CodeArea codeArea = currentTab.getCodeArea();

            // Update the base style with new font size
            codeArea.setStyle(
                    "-fx-background-color: " + BACKGROUND_COLOR + ";" +
                            "-fx-font-family: 'Monospace';" +
                            "-fx-font-size: " + fontSize + "px;" +
                            "-fx-text-fill: " + TEXT_COLOR + ";"
            );

            // Update the global font size
            currentFontSize = fontSize;

            // Reapply auto-styling after font size change
            Platform.runLater(() -> applyAutoStyling(codeArea));

            // Cancel any existing font size display timer
            if (fontSizeDisplayTimer != null) {
                fontSizeDisplayTimer.cancel();
                fontSizeDisplayTimer = null;
            }

            // Save current cursor position info
            currentTab = getCurrentEditorTab();
            int line = currentTab != null ? currentTab.getCodeArea().getCurrentParagraph() + 1 : 1;
            int column = currentTab != null ? currentTab.getCodeArea().getCaretColumn() + 1 : 1;

            // Show font size in status bar temporarily
            Platform.runLater(() -> statusLabel.setText("Font size: " + currentFontSize + "px"));

            // Create new timer to restore cursor position after delay
            fontSizeDisplayTimer = new Timer();
            fontSizeDisplayTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> statusLabel.setText("Line: " + line + ", Column: " + column));
                }
            }, 3000);

            // Ensure the codeArea maintains focus
            Platform.runLater(codeArea::requestFocus);
        }
    }


    /**
     * Sets up keyboard shortcuts
     */
    private void setupKeyboardShortcuts(Scene scene, Stage stage) {
        // Add at the top of the method:
        // Define shortcuts
        KeyCombination keyCombNew = new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombOpen = new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombSave = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCut = new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCopy = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombPaste = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombExit = new KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombCloseTab = new KeyCodeCombination(KeyCode.W, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombClipboard = new KeyCodeCombination(KeyCode.V, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN);

        // Font size shortcuts
        KeyCombination keyCombIncFontPlus = new KeyCodeCombination(KeyCode.PLUS, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombIncFontEquals = new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombDecFont = new KeyCodeCombination(KeyCode.MINUS, KeyCombination.CONTROL_DOWN);
        KeyCombination keyCombResetFont = new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.CONTROL_DOWN);

        // Keep your existing shortcuts
        scene.getAccelerators().put(keyCombNew, this::createNewTab);
        scene.getAccelerators().put(keyCombOpen, () -> openFile(stage));
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

        // Add font size shortcuts
        scene.getAccelerators().put(keyCombIncFontPlus, () -> {
            if (currentFontSize < 36) { // Maximum size
                setFontSize(currentFontSize + 2);
            }
        });

        scene.getAccelerators().put(keyCombIncFontEquals, () -> {
            if (currentFontSize < 36) { // Maximum size
                setFontSize(currentFontSize + 2);
            }
        });

        scene.getAccelerators().put(keyCombDecFont, () -> {
            if (currentFontSize > 8) { // Minimum size
                setFontSize(currentFontSize - 2);
            }
        });

        scene.getAccelerators().put(keyCombResetFont, () -> {
            setFontSize(14); // Reset to default
        });

        // Keep your existing handler for Ctrl+Tab to switch tabs
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

        // And the rest of your existing shortcuts...
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
        // This should be a method that displays your clipboard history
        scene.getAccelerators().put(keyCombClipboard, this::showClipboardPopup);
    }

    private void showClipboardPopup() {
        if (clipboardHistory.isEmpty()) return;

        // Store reference to current editor tab before opening popup
        EditorTab currentEditorTab = getCurrentEditorTab();

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
                if (selected != null && currentEditorTab != null) {
                    CodeArea codeArea = currentEditorTab.getCodeArea();
                    int caretPos = codeArea.getCaretPosition();
                    codeArea.insertText(caretPos, selected);

                    // Close the popup first
                    ((Stage) listView.getScene().getWindow()).close();

                    // Then restore focus and caret visibility with proper timing
                    Platform.runLater(() -> {
                        // Request focus on the code area
                        codeArea.requestFocus();

                        // Force a refresh of the caret by briefly moving it
                        Platform.runLater(() -> {
                            int currentPos = codeArea.getCaretPosition();
                            // Move caret to trigger visual refresh
                            codeArea.moveTo(currentPos);
                            // Ensure the area is focused and caret is visible
                            codeArea.requestFocus();
                            // Request follow caret to ensure visibility
                            codeArea.requestFollowCaret();
                        });
                    });
                } else {
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

        // Add a close handler to restore focus when popup is closed without selection
        popupStage.setOnHidden(e -> {
            if (currentEditorTab != null) {
                Platform.runLater(() -> {
                    CodeArea codeArea = currentEditorTab.getCodeArea();
                    codeArea.requestFocus();
                    // Force caret refresh
                    Platform.runLater(() -> {
                        int currentPos = codeArea.getCaretPosition();
                        codeArea.moveTo(currentPos);
                        codeArea.requestFocus();
                        codeArea.requestFollowCaret();
                    });
                });
            }
        });

        popupStage.showAndWait();
    }





    /**
     * Shows a dialog for finding text in the current document
     */
    private void initializeFindBar(StackPane stack) {
        searchField = new TextField();
        searchField.setPromptText("Find...");
        searchField.setPrefWidth(250);  // Slightly wider for better usability
        searchField.setStyle(
                "-fx-background-color: #3A3A4A;" +
                        "-fx-text-fill: #E0E0E0;" +
                        "-fx-prompt-text-fill: #8888AA;" +
                        "-fx-border-color: #4D4D6D;" +
                        "-fx-border-radius: 4px;" +
                        "-fx-background-radius: 4px;" +
                        "-fx-padding: 5px 8px;" +
                        "-fx-font-family: 'Segoe UI', sans-serif;" +
                        "-fx-font-size: 13px;"
        );

        Button closeBtn = new Button("✕");
        closeBtn.setOnAction(e -> {
            findBar.setVisible(false);
            EditorTab currentTab = getCurrentEditorTab();
            if (currentTab != null) {
                currentTab.getCodeArea().requestFocus();
            }
        });
        closeBtn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: #BB86FC;" +
                        "-fx-font-size: 14px;" +
                        "-fx-padding: 5px 8px;" +
                        "-fx-cursor: hand;"
        );
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: #3A3A5A; -fx-text-fill: #FF79C6;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BB86FC;"));

        Button nextBtn = new Button("↓");
        nextBtn.setStyle(
                "-fx-background-color: #4A4A6A;" +
                        "-fx-text-fill: #E0E0E0;" +
                        "-fx-background-radius: 3px;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        nextBtn.setOnMouseEntered(e -> nextBtn.setStyle("-fx-background-color: #5A5A7A; -fx-text-fill: #FFFFFF;"));
        nextBtn.setOnMouseExited(e -> nextBtn.setStyle("-fx-background-color: #4A4A6A; -fx-text-fill: #E0E0E0;"));

        Button prevBtn = new Button("↑");
        prevBtn.setStyle(
                "-fx-background-color: #4A4A6A;" +
                        "-fx-text-fill: #E0E0E0;" +
                        "-fx-background-radius: 3px;" +
                        "-fx-padding: 5px 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;"
        );
        prevBtn.setOnMouseEntered(e -> prevBtn.setStyle("-fx-background-color: #5A5A7A; -fx-text-fill: #FFFFFF;"));
        prevBtn.setOnMouseExited(e -> prevBtn.setStyle("-fx-background-color: #4A4A6A; -fx-text-fill: #E0E0E0;"));

        // Setup search navigation buttons
        nextBtn.setOnAction(e -> findNext(searchField.getText()));
        prevBtn.setOnAction(e -> findPrevious(searchField.getText()));

        findBar = new HBox(5, searchField, prevBtn, nextBtn, closeBtn);
        findBar.setPadding(new Insets(5));
        findBar.setMaxHeight(40);  // Slightly taller
        findBar.setMaxWidth(Region.USE_PREF_SIZE);
        findBar.setStyle(
                "-fx-background-color: #2A2A3A;" +
                        "-fx-border-color: #3D3D5D;" +
                        "-fx-border-width: 0 0 1px 0;" +
                        "-fx-background-radius: 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 5, 0, 0, 1);"
        );
        findBar.setAlignment(Pos.CENTER_LEFT);
        findBar.setVisible(false);

        // Position in top-right with proper margins
        StackPane.setAlignment(findBar, Pos.TOP_RIGHT);
        StackPane.setMargin(findBar, new Insets(10, 15, 0, 0));

        // Add to stack pane on top of the content
        stack.getChildren().add(findBar);
        findBar.toFront();

        // Handle search logic on Enter key
        searchField.setOnAction(e -> findNext(searchField.getText()));

        // Add keyboard handler to close find bar on ESC
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                findBar.setVisible(false);
                EditorTab currentTab = getCurrentEditorTab();
                if (currentTab != null) {
                    currentTab.getCodeArea().requestFocus();
                }
                event.consume();
            }
        });

        // Add listener to show/hide find bar
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                searchField.setStyle(
                        "-fx-background-color: #3A3A4A;" +
                                "-fx-text-fill: #E0E0E0;" +
                                "-fx-border-color: #4D4D6D;"
                );
            }
        });
    }

    // Helper method for finding the next occurrence
    private void findNext(String query) {
        if (query == null || query.isEmpty()) return;

        CodeArea codeArea = Objects.requireNonNull(getCurrentEditorTab()).getCodeArea();
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

        CodeArea codeArea = Objects.requireNonNull(getCurrentEditorTab()).getCodeArea();
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