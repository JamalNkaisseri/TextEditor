package com.texteditor.ui;

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

        // Store the original caret position and selection
        final int originalCaretPos = codeArea.getCaretPosition();
        final int originalAnchor = codeArea.getAnchor();

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

        // Improved focus restoration for RichTextFX CodeArea
        Runnable restoreFocusAndCaret = () -> {
            // Use multiple delayed Platform.runLater calls to ensure proper focus restoration
            Platform.runLater(() -> {
                codeArea.requestFocus();
                Platform.runLater(() -> {
                    // Force caret to be visible by moving it slightly and then back
                    int currentPos = codeArea.getCaretPosition();
                    if (currentPos > 0) {
                        codeArea.moveTo(currentPos - 1);
                        codeArea.moveTo(currentPos);
                    } else if (codeArea.getLength() > 0) {
                        codeArea.moveTo(1);
                        codeArea.moveTo(0);
                    }

                    // Ensure the area is focused and caret is blinking
                    codeArea.requestFocus();

                    Platform.runLater(() -> {
                        // Final focus attempt to ensure caret visibility
                        codeArea.requestFocus();
                        // Trigger a layout pass to ensure caret is rendered
                        codeArea.requestFollowCaret();
                    });
                });
            });
        };

        // Helper to close popup and restore focus
        Runnable closeAndRestore = () -> {
            popupStage.close();
            restoreFocusAndCaret.run();
        };

        // On double-click: insert selected text
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Platform.runLater(() -> {
                        int caretPos = codeArea.getCaretPosition();
                        codeArea.insertText(caretPos, selected);
                        closeAndRestore.run();
                    });
                }
            }
        });

        // Support Enter key to select item
        listView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                String selected = listView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    Platform.runLater(() -> {
                        int caretPos = codeArea.getCaretPosition();
                        codeArea.insertText(caretPos, selected);
                        closeAndRestore.run();
                    });
                }
            } else if (event.getCode() == KeyCode.ESCAPE) {
                popupStage.close();
                // Restore original position when cancelled
                Platform.runLater(() -> {
                    codeArea.moveTo(originalAnchor, originalCaretPos);
                    restoreFocusAndCaret.run();
                });
            }
        });

        // Critical: Handle the close event properly for CodeArea
        popupStage.setOnCloseRequest(e -> {
            // Don't prevent closing, but ensure proper cleanup
            Platform.runLater(() -> {
                // Restore original caret position if nothing was selected
                codeArea.moveTo(originalAnchor, originalCaretPos);
                restoreFocusAndCaret.run();
            });
        });

        // Show the popup (non-blocking)
        popupStage.show();

        // Focus the ListView and select first item
        Platform.runLater(() -> {
            listView.requestFocus();
            if (!listView.getItems().isEmpty()) {
                listView.getSelectionModel().select(0);
            }
        });
    }
}