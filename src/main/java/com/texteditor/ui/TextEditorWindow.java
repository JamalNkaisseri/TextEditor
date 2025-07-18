package com.texteditor.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TextEditorWindow {

    private Label statusLabel;
    private TabManager tabManager;
    private FileManager fileManager;
    private final List<String> clipboardHistory = new ArrayList<>();
    private double fontSize = 14;
    private boolean wordWrapEnabled = true; // Default to enabled

    private SearchBar searchBar;
    private SearchTool searchTool;

    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // Status bar
        statusLabel = new Label("Line: 1, Column: 1");
        statusLabel.setStyle(
                "-fx-background-color: #2A2A3A;" +
                        "-fx-text-fill: #BBBBBB;" +
                        "-fx-font-size: 12px;" +
                        "-fx-padding: 5px 15px;" +
                        "-fx-border-width: 1px 0 0 0;" +
                        "-fx-border-color: #3D3D4D;"
        );
        root.setBottom(statusLabel);

        // Scene setup
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/dark-theme.css")).toExternalForm());

        // Tab and file managers - Fixed parameter order
        fileManager = new FileManager(stage);
        tabManager = new TabManager(scene, stage, fileManager); // ✅ Corrected parameter order

        root.setCenter(tabManager.getTabPane());

        // Setup search bar
        searchBar = new SearchBar();
        searchBar.setVisible(false);
        BorderPane.setMargin(searchBar, new Insets(5));
        root.setTop(searchBar);

        // Initialize search tool with current code area
        updateSearchTool();

        searchBar.setOnClose(() -> searchBar.setVisible(false));
        searchBar.getSearchField().setOnAction(e -> {
            if (searchTool != null) searchTool.findNext();
        });
        searchBar.getNextButton().setOnAction(e -> {
            if (searchTool != null) searchTool.findNext();
        });
        searchBar.getPrevButton().setOnAction(e -> {
            if (searchTool != null) searchTool.findPrevious();
        });

        // Register all shortcuts
        registerShortcuts(scene);

        // Setup caret and tab tracking
        setupTracking();

        // Handle application close event
        stage.setOnCloseRequest(e -> {
            if (!handleApplicationClose()) {
                e.consume(); // Prevent closing if user cancels
            }
        });

        // Show editor
        stage.setTitle("TrickyTeta");
        stage.setScene(scene);
        stage.show();
    }

    private void registerShortcuts(Scene scene) {
        ShortcutManager manager = new ShortcutManager();

        manager.register(ShortcutManager.CUT, () -> {
            CodeArea area = tabManager.getCurrentCodeArea();
            if (area != null) {
                String selected = area.getSelectedText();
                if (!selected.isEmpty()) addToClipboardHistory(selected);
                area.cut();
            }
        });

        manager.register(ShortcutManager.COPY, () -> {
            CodeArea area = tabManager.getCurrentCodeArea();
            if (area != null) {
                String selected = area.getSelectedText();
                if (!selected.isEmpty()) addToClipboardHistory(selected);
                area.copy();
            }
        });

        manager.register(ShortcutManager.PASTE, () -> {
            CodeArea area = tabManager.getCurrentCodeArea();
            if (area != null) area.paste();
        });

        manager.register(ShortcutManager.SHOW_CLIPBOARD_HISTORY, () -> {
            CodeArea area = tabManager.getCurrentCodeArea();
            if (area != null) ClipboardPopup.show(clipboardHistory, area);
        });

        manager.register(ShortcutManager.INCREASE_FONT_PLUS, this::increaseFontSize);
        manager.register(ShortcutManager.INCREASE_FONT_EQUALS, this::increaseFontSize);
        manager.register(ShortcutManager.DECREASE_FONT, this::decreaseFontSize);
        manager.register(ShortcutManager.RESET_FONT, this::resetFontSize);

        manager.register(ShortcutManager.SAVE_FILE, () -> fileManager.saveFile(tabManager));
        manager.register(ShortcutManager.SAVE_AS_FILE, () -> fileManager.saveFileAs(tabManager));
        manager.register(ShortcutManager.EXIT_APP, this::handleApplicationExit);

        // Ctrl+F to open search bar
        manager.register(KeyCombination.valueOf("Ctrl+F"), () -> {
            searchBar.setVisible(true);
            searchBar.focusField();
        });

        // Toggle word wrap with Alt+W
        manager.register(KeyCombination.valueOf("Alt+W"), this::toggleWordWrap);

        manager.register(ShortcutManager.OPEN_FILE, () -> fileManager.openFile(tabManager));

        manager.attachTo(scene);
    }

    private void addToClipboardHistory(String text) {
        clipboardHistory.add(0, text);
        if (clipboardHistory.size() > 50) {
            clipboardHistory.remove(clipboardHistory.size() - 1);
        }
    }

    private void increaseFontSize() {
        fontSize += 2;
        updateFontSize();
    }

    private void decreaseFontSize() {
        fontSize = Math.max(8, fontSize - 2);
        updateFontSize();
    }

    private void resetFontSize() {
        fontSize = 14;
        updateFontSize();
    }

    private void updateFontSize() {
        CodeArea area = tabManager.getCurrentCodeArea();
        if (area != null) {
            area.setStyle("-fx-font-size: " + fontSize + "px;");
        }
    }

    /**
     * Toggles word wrap for the current code area
     */
    private void toggleWordWrap() {
        wordWrapEnabled = !wordWrapEnabled;
        CodeArea area = tabManager.getCurrentCodeArea();
        if (area != null) {
            applyWordWrap(area);
        }

        // Update status bar to show word wrap status
        updateStatusBarWithWordWrap();
    }

    /**
     * Applies word wrap setting to a code area
     */
    private void applyWordWrap(CodeArea codeArea) {
        if (codeArea != null) {
            codeArea.setWrapText(wordWrapEnabled);
        }
    }

    /**
     * Gets the current word wrap state
     */
    public boolean isWordWrapEnabled() {
        return wordWrapEnabled;
    }

    /**
     * Sets word wrap state programmatically
     */
    public void setWordWrapEnabled(boolean enabled) {
        this.wordWrapEnabled = enabled;
        CodeArea area = tabManager.getCurrentCodeArea();
        if (area != null) {
            applyWordWrap(area);
        }
        updateStatusBarWithWordWrap();
    }

    private void updateSearchTool() {
        CodeArea codeArea = tabManager.getCurrentCodeArea();
        if (codeArea != null) {
            searchTool = new SearchTool(codeArea, searchBar.getSearchField());
        }
    }

    private void setupTracking() {
        CodeArea currentArea = tabManager.getCurrentCodeArea();
        if (currentArea != null) {
            bindCodeArea(currentArea, tabManager.getTabPane().getSelectionModel().getSelectedItem());
        }

        // Listen for tab changes
        tabManager.getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            CodeArea newArea = tabManager.getCurrentCodeArea();
            if (newArea != null) {
                bindCodeArea(newArea, newTab);
                updateStatusBar(newArea);
                updateSearchTool(); // Update search tool for new tab
                applyFontSize(newArea); // Apply current font size to new tab
                applyWordWrap(newArea); // Apply word wrap to new tab
            }
        });

        // Listen for new tabs being added
        tabManager.getTabPane().getTabs().addListener((javafx.collections.ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tab addedTab : change.getAddedSubList()) {
                        // Apply font size and word wrap to newly created tabs
                        Platform.runLater(() -> {
                            CodeArea area = tabManager.getCurrentCodeArea();
                            if (area != null && tabManager.getTabPane().getSelectionModel().getSelectedItem() == addedTab) {
                                applyFontSize(area);
                                applyWordWrap(area);
                            }
                        });
                    }
                }
            }
        });
    }

    private void bindCodeArea(CodeArea codeArea, Tab tab) {
        AutoStyler styler = new AutoStyler();
        styler.bindTo(codeArea);

        codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> updateStatusBar(codeArea));

        // Apply current font size and word wrap to this code area
        applyFontSize(codeArea);
        applyWordWrap(codeArea);
    }

    private void applyFontSize(CodeArea codeArea) {
        if (codeArea != null) {
            codeArea.setStyle("-fx-font-size: " + fontSize + "px;");
        }
    }

    private void updateStatusBar(CodeArea codeArea) {
        if (codeArea != null) {
            int line = codeArea.getCurrentParagraph() + 1;
            int column = codeArea.getCaretColumn() + 1;
            String wrapStatus = wordWrapEnabled ? " | Word Wrap: ON" : " | Word Wrap: OFF";
            statusLabel.setText("Line: " + line + ", Column: " + column + wrapStatus);
        }
    }

    private void updateStatusBarWithWordWrap() {
        CodeArea codeArea = tabManager.getCurrentCodeArea();
        if (codeArea != null) {
            updateStatusBar(codeArea);
        }
    }

    /**
     * Handles application exit (Ctrl+Q) - checks all tabs for unsaved changes
     */
    private void handleApplicationExit() {
        if (handleApplicationClose()) {
            Platform.exit();
        }
    }

    /**
     * Checks all tabs for unsaved changes and prompts user for each dirty tab
     * @return true if it's safe to close the application, false if user cancelled
     */
    private boolean handleApplicationClose() {
        List<Tab> dirtyTabs = new ArrayList<>();

        // Find all tabs with unsaved changes
        for (Tab tab : tabManager.getTabPane().getTabs()) {
            if (tab.getText().startsWith("*")) {
                dirtyTabs.add(tab);
            }
        }

        // If no dirty tabs, it's safe to close
        if (dirtyTabs.isEmpty()) {
            return true;
        }

        // Process each dirty tab
        for (Tab dirtyTab : dirtyTabs) {
            // Select the tab so user can see which file they're being asked about
            tabManager.getTabPane().getSelectionModel().select(dirtyTab);

            String cleanTitle = dirtyTab.getText().substring(1); // Remove the * prefix
            UnsavedChangesDialog.Result result = UnsavedChangesDialog.show(cleanTitle,
                    (Stage) tabManager.getTabPane().getScene().getWindow());

            if (result == UnsavedChangesDialog.Result.SAVE) {
                // Save the file and check if it was successful
                boolean saveSuccess = fileManager.saveFile(tabManager);
                if (!saveSuccess) {
                    // User cancelled the save dialog or save failed
                    return false;
                }

                // Update the original content in com.texteditor.TabManager
                CodeArea codeArea = tabManager.getCurrentCodeArea();
                if (codeArea != null) {
                    tabManager.updateOriginalContent(dirtyTab, codeArea.getText());
                }
            } else if (result == UnsavedChangesDialog.Result.CANCEL) {
                // User cancelled, don't close the application
                return false;
            }
            // If DONT_SAVE, continue to next tab
        }

        // All tabs processed successfully
        return true;
    }
}