package com.texteditor;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TabManager {

    private final TabPane tabPane = new TabPane();
    private final Map<Tab, CodeArea> codeAreas = new HashMap<>();
    private final Map<Tab, String> originalContent = new HashMap<>(); // Track original content
    private final Set<Integer> usedNumbers = new HashSet<>();
    private final Scene scene;
    private final Stage stage;
    private final FileManager fileManager;

    public TabManager(Scene scene, Stage stage, FileManager fileManager) {
        this.scene = scene;
        this.stage = stage;
        this.fileManager = fileManager;

        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        createNewTab();

        ShortcutManager shortcutManager = new ShortcutManager();
        shortcutManager.register(ShortcutManager.NEW_TAB, this::createNewTab);
        shortcutManager.register(ShortcutManager.CLOSE_TAB, this::closeCurrentTab);
        shortcutManager.attachTo(scene);
    }

    public TabPane getTabPane() {
        return tabPane;
    }

    public CodeArea getCurrentCodeArea() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        return selected != null ? codeAreas.get(selected) : null;
    }

    public void createNewTab() {
        CodeArea codeArea = new CodeArea();

        codeArea.setParagraphGraphicFactory(line -> {
            Label lineNo = new Label(String.valueOf(line + 1));
            lineNo.getStyleClass().add("line-number");
            return lineNo;
        });

        String tabTitle = generateTabTitle();
        Tab tab = new Tab("*" + tabTitle);

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        tab.setContent(scrollPane);

        codeAreas.put(tab, codeArea);
        originalContent.put(tab, ""); // New tab starts with empty content
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        // Add change listener to detect modifications
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            String original = originalContent.get(tab);
            if (original != null && !newText.equals(original)) {
                fileManager.markDirty(tab);
            } else if (original != null && newText.equals(original)) {
                // Content matches original, remove dirty marker
                cleanTabTitle(tab);
            }
        });

        tab.setOnCloseRequest(e -> {
            if (tab.getText().startsWith("*")) {
                String cleanTitle = tab.getText().substring(1); // Remove the * prefix
                UnsavedChangesDialog.Result result = UnsavedChangesDialog.show(cleanTitle, stage);

                if (result == UnsavedChangesDialog.Result.SAVE) {
                    boolean saveSuccess = fileManager.saveFile(this);
                    if (!saveSuccess) {
                        // Save was cancelled or failed, don't close the tab
                        e.consume();
                        return;
                    }
                    // Save successful, update original content
                    originalContent.put(tab, codeArea.getText());
                } else if (result == UnsavedChangesDialog.Result.CANCEL) {
                    e.consume(); // Prevent tab from closing
                    return;
                }
                // If DONT_SAVE, let the tab close normally
            }
        });

        tab.setOnClosed(e -> {
            String title = tab.getText();
            if (title.startsWith("*")) {
                title = title.substring(1);
            }
            int num = extractUntitledNumber(title);
            usedNumbers.remove(num);
            codeAreas.remove(tab);
            originalContent.remove(tab);
        });
    }

    private String generateTabTitle() {
        if (!usedNumbers.contains(0)) {
            usedNumbers.add(0);
            return "Untitled";
        }

        int n = 1;
        while (usedNumbers.contains(n)) n++;
        usedNumbers.add(n);
        return "Untitled " + n;
    }

    private int extractUntitledNumber(String title) {
        if (title.equals("Untitled")) return 0;
        if (title.startsWith("Untitled ")) {
            try {
                return Integer.parseInt(title.substring(9).trim());
            } catch (NumberFormatException ignored) {}
        }
        return -1;
    }

    private void cleanTabTitle(Tab tab) {
        String title = tab.getText();
        if (title.startsWith("*")) {
            tab.setText(title.substring(1));
        }
    }

    public void closeCurrentTab() {
        Tab selected = tabPane.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // This will trigger the onCloseRequest handler
            tabPane.getTabs().remove(selected);
        }
    }

    public Tab createNewTabWithContent(String content, String title) {
        CodeArea codeArea = new CodeArea();
        codeArea.replaceText(content);

        codeArea.setParagraphGraphicFactory(line -> {
            Label lineNo = new Label(String.valueOf(line + 1));
            lineNo.getStyleClass().add("line-number");
            return lineNo;
        });

        Tab tab = new Tab(title);
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        tab.setContent(scrollPane);

        codeAreas.put(tab, codeArea);
        originalContent.put(tab, content); // Store original content from file
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        // Add change listener for opened files too
        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            String original = originalContent.get(tab);
            if (original != null && !newText.equals(original)) {
                fileManager.markDirty(tab);
            } else if (original != null && newText.equals(original)) {
                cleanTabTitle(tab);
            }
        });

        tab.setOnCloseRequest(e -> {
            if (tab.getText().startsWith("*")) {
                String cleanTitle = tab.getText().substring(1);
                UnsavedChangesDialog.Result result = UnsavedChangesDialog.show(cleanTitle, stage);

                if (result == UnsavedChangesDialog.Result.SAVE) {
                    boolean saveSuccess = fileManager.saveFile(this);
                    if (!saveSuccess) {
                        // Save was cancelled or failed, don't close the tab
                        e.consume();
                        return;
                    }
                    originalContent.put(tab, codeArea.getText());
                } else if (result == UnsavedChangesDialog.Result.CANCEL) {
                    e.consume();
                    return;
                }
            }
        });

        tab.setOnClosed(e -> {
            codeAreas.remove(tab);
            originalContent.remove(tab);
        });

        return tab;
    }

    // Method to update original content after successful save
    public void updateOriginalContent(Tab tab, String content) {
        originalContent.put(tab, content);
    }
}