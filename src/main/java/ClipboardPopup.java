import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.fxmisc.richtext.CodeArea;

import java.util.List;

public class ClipboardPopup {

    /**
     * Displays a clipboard history popup and inserts the selected item at the caret.
     */
    public static void show(List<String> clipboardHistory, CodeArea codeArea) {
        if (clipboardHistory.isEmpty() || codeArea == null) return;

        ListView<String> listView = new ListView<>();
        listView.getItems().addAll(clipboardHistory);

        // Match dark theme styling
        listView.setStyle(
                "-fx-control-inner-background: #1E1E2E;" +
                        "-fx-background-color: #1E1E2E;" +
                        "-fx-border-color: #3D3D4D;" +
                        "-fx-selection-bar: #444;" +
                        "-fx-selection-bar-text: #DDDDDD;"
        );

        VBox container = new VBox(listView);
        container.setStyle("-fx-background-color: #1E1E2E; -fx-padding: 10;");

        Scene scene = new Scene(container, 400, 300);
        scene.setFill(Color.web("#1E1E2E"));

        Stage popupStage = new Stage(StageStyle.DECORATED);
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Clipboard History");
        popupStage.setScene(scene);

        // On double-click: insert selected text
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    int caretPos = codeArea.getCaretPosition();
                    codeArea.insertText(caretPos, selected);
                    popupStage.close();

                    // Then restore focus and caret visibility with proper timing
                    Platform.runLater(() -> {
                        codeArea.requestFocus();
                        Platform.runLater(() -> {
                            int currentPos = codeArea.getCaretPosition();
                            codeArea.moveTo(currentPos);
                            codeArea.requestFocus();
                            codeArea.requestFollowCaret();
                        });
                    });
                }
            }
        });

        // Optional: support Esc key to close popup
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                popupStage.close();
            }
        });

        // On popup close: restore focus
        popupStage.setOnHidden(e -> {
            Platform.runLater(() -> {
                codeArea.requestFocus();
                Platform.runLater(() -> {
                    int currentPos = codeArea.getCaretPosition();
                    codeArea.moveTo(currentPos);
                    codeArea.requestFocus();
                    codeArea.requestFollowCaret();
                });
            });
        });

        popupStage.showAndWait();
    }
}
