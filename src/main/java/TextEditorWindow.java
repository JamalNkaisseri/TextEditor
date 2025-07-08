import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

import java.util.Objects;

public class TextEditorWindow {

    private Label statusLabel; // Bottom-left status bar

    public void start(Stage stage) {
        // Root layout for window
        BorderPane root = new BorderPane();

        // Create the code editor
        CodeArea codeArea = new CodeArea();

        // Add line numbers with custom style
        codeArea.setParagraphGraphicFactory(line -> {
            Label lineNo = new Label(String.valueOf(line + 1));
            lineNo.getStyleClass().add("line-number");
            return lineNo;
        });

        // Track and display current line/column
        codeArea.caretPositionProperty().addListener((obs, oldVal, newVal) -> updateStatusBar(codeArea));

        // Wrap editor in a scrollable container
        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);
        root.setCenter(scrollPane);

        // Create and style the status bar
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

        // Scene and theming
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/dark-theme.css")).toExternalForm());

        // Finalize window
        stage.setTitle("TrickyTeta");
        stage.setScene(scene);
        stage.show();
    }

    // Update line/column in status bar
    private void updateStatusBar(CodeArea codeArea) {
        int line = codeArea.getCurrentParagraph() + 1;
        int column = codeArea.getCaretColumn() + 1;
        statusLabel.setText("Line: " + line + ", Column: " + column);
    }
}
