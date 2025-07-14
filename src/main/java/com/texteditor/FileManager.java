package com.texteditor;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Tab;
import org.fxmisc.richtext.CodeArea;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileManager {

    private final Map<Tab, Path> fileMap = new HashMap<>();
    private final Stage stage;

    public FileManager(Stage stage) {
        this.stage = stage;
    }

    /**
     * Opens a Save As dialog and saves content to a new file (any type).
     * @return true if file was saved successfully, false if cancelled or failed
     */
    public boolean saveFileAs(TabManager tabManager) {
        Tab currentTab = tabManager.getTabPane().getSelectionModel().getSelectedItem();
        CodeArea area = tabManager.getCurrentCodeArea();
        if (currentTab == null || area == null) return false; // Fixed: added return value

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File As");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*.*") // Accept all types
        );

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Path path = file.toPath();
            try {
                String content = area.getText();
                Files.writeString(path, content);

                fileMap.put(currentTab, path);
                currentTab.setText(path.getFileName().toString()); // Remove dirty marker

                // Update the original content in com.texteditor.TabManager
                tabManager.updateOriginalContent(currentTab, content);
                return true; // Save successful
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: Show error dialog to user
                return false; // Save failed
            }
        }
        return false; // User cancelled
    }

    /**
     * Saves the current file if it has a known path, otherwise falls back to Save As.
     * @return true if file was saved successfully, false if cancelled or failed
     */
    public boolean saveFile(TabManager tabManager) {
        Tab currentTab = tabManager.getTabPane().getSelectionModel().getSelectedItem();
        CodeArea area = tabManager.getCurrentCodeArea();
        if (currentTab == null || area == null) return false; // Fixed: added return value

        Path path = fileMap.get(currentTab);

        if (path == null) {
            return saveFileAs(tabManager); // First-time save - return result
        }

        try {
            String content = area.getText();
            Files.writeString(path, content);

            // Clean the tab title (remove * prefix)
            String title = currentTab.getText();
            if (title.startsWith("*")) {
                currentTab.setText(title.substring(1));
            }

            // Update the original content in com.texteditor.TabManager
            tabManager.updateOriginalContent(currentTab, content);
            return true; // Save successful
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: Show error dialog to user
            return false; // Save failed
        }
    }

    /**
     * Marks a tab dirty by prefixing its title with '*'
     */
    public void markDirty(Tab tab) {
        if (!tab.getText().startsWith("*")) {
            tab.setText("*" + tab.getText());
        }
    }

    public void openFile(TabManager tabManager) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                String content = Files.readString(file.toPath());
                Tab tab = tabManager.createNewTabWithContent(content, file.getName());
                fileMap.put(tab, file.toPath());
            } catch (IOException e) {
                e.printStackTrace(); // TODO: Show error dialog to user
            }
        }
    }

    /**
     * Gets the file path associated with a tab
     */
    public Path getFilePath(Tab tab) {
        return fileMap.get(tab);
    }

    /**
     * Checks if a tab has an associated file
     */
    public boolean hasFile(Tab tab) {
        return fileMap.containsKey(tab);
    }
}