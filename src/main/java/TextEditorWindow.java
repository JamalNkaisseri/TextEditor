import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.util.Objects;

public class TextEditorWindow {

    private Label statusLabel;

    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        // Tabs
        TabManager tabManager = new TabManager();
        root.setCenter(tabManager.getTabPane());

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

        // Track caret movement in current tab
        CodeArea currentCodeArea = tabManager.getCurrentCodeArea();
        if (currentCodeArea != null) {
            currentCodeArea.caretPositionProperty().addListener((obs, oldVal, newVal) ->
                    updateStatusBar(currentCodeArea));
        }

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/dark-theme.css")).toExternalForm());

        stage.setTitle("TrickyTeta");
        stage.setScene(scene);
        stage.show();
    }

    private void updateStatusBar(CodeArea codeArea) {
        int line = codeArea.getCurrentParagraph() + 1;
        int column = codeArea.getCaretColumn() + 1;
        statusLabel.setText("Line: " + line + ", Column: " + column);
    }
}
