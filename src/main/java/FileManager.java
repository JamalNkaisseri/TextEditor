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
     */
    public void saveFileAs(TabManager tabManager) {
        Tab currentTab = tabManager.getTabPane().getSelectionModel().getSelectedItem();
        CodeArea area = tabManager.getCurrentCodeArea();
        if (currentTab == null || area == null) return;

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
                Files.writeString(path, area.getText());

                fileMap.put(currentTab, path);
                currentTab.setText(path.getFileName().toString()); // Remove dirty marker
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves the current file if it has a known path, otherwise falls back to Save As.
     */
    public void saveFile(TabManager tabManager) {
        Tab currentTab = tabManager.getTabPane().getSelectionModel().getSelectedItem();
        CodeArea area = tabManager.getCurrentCodeArea();
        if (currentTab == null || area == null) return;

        Path path = fileMap.get(currentTab);

        if (path == null) {
            saveFileAs(tabManager); // First-time save
            return;
        }

        try {
            Files.writeString(path, area.getText());
            currentTab.setText(path.getFileName().toString()); // Clean tab title
        } catch (IOException e) {
            e.printStackTrace();
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
}
