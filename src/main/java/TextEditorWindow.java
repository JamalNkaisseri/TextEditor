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

        // Tab and file managers
        tabManager = new TabManager(scene);
        fileManager = new FileManager(stage);
        root.setCenter(tabManager.getTabPane());

        // Setup search bar
        CodeArea codeArea = tabManager.getCurrentCodeArea();
        searchBar = new SearchBar();
        searchBar.setVisible(false);
        BorderPane.setMargin(searchBar, new Insets(5));
        root.setTop(searchBar);

        if (codeArea != null) {
            searchTool = new SearchTool(codeArea, searchBar.getSearchField());
        }

        searchBar.setOnClose(() -> searchBar.setVisible(false));
        searchBar.getSearchField().setOnAction(e -> searchTool.findNext());
        searchBar.getNextButton().setOnAction(e -> searchTool.findNext());
        searchBar.getPrevButton().setOnAction(e -> searchTool.findPrevious());

        // Register all shortcuts
        registerShortcuts(scene);

        // Setup caret and tab tracking
        setupTracking();

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
        manager.register(ShortcutManager.EXIT_APP, Platform::exit);

        // Ctrl+F to open search bar
        manager.register(KeyCombination.valueOf("Ctrl+F"), () -> {
            searchBar.setVisible(true);
            searchBar.focusField();
        });

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

    private void setupTracking() {
        if (tabManager.getCurrentCodeArea() != null) {
            bindCodeArea(tabManager.getCurrentCodeArea(), tabManager.getTabPane().getSelectionModel().getSelectedItem());
        }

        tabManager.getTabPane().getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            CodeArea newArea = tabManager.getCurrentCodeArea();
            if (newArea != null) {
                bindCodeArea(newArea, newTab);
                updateStatusBar(newArea);
            }
        });
    }

    private void bindCodeArea(CodeArea codeArea, Tab tab) {
        AutoStyler styler = new AutoStyler();
        styler.bindTo(codeArea);

        codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> updateStatusBar(codeArea));
    }

    private void updateStatusBar(CodeArea codeArea) {
        int line = codeArea.getCurrentParagraph() + 1;
        int column = codeArea.getCaretColumn() + 1;
        statusLabel.setText("Line: " + line + ", Column: " + column);
    }
}
