package com.texteditor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.concurrent.atomic.AtomicReference;

public class UnsavedChangesDialog {

    public enum Result {
        SAVE, DONT_SAVE, CANCEL
    }

    public static Result show(String filename, Stage owner) {
        // Use AtomicReference to avoid static variable issues
        AtomicReference<Result> userChoice = new AtomicReference<>(Result.CANCEL);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle("Unsaved Changes");
        dialog.setResizable(false);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #2A2A3A; -fx-border-color: #444; -fx-border-width: 1px; -fx-background-radius: 6; -fx-border-radius: 6;");

        Label title = new Label("Unsaved Changes");
        title.setStyle("-fx-text-fill: #F5C2E7; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label message = new Label("Do you want to save changes to \"" + filename + "\"?");
        message.setStyle("-fx-text-fill: #DDDDDD; -fx-font-size: 13px;");
        message.setWrapText(true);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button saveButton = styledButton("Save", "#89B4FA");
        Button dontSaveButton = styledButton("Don't Save", "#F38BA8");
        Button cancelButton = styledButton("Cancel", "#A6ADC8");

        saveButton.setOnAction(e -> {
            userChoice.set(Result.SAVE);
            dialog.close();
        });

        dontSaveButton.setOnAction(e -> {
            userChoice.set(Result.DONT_SAVE);
            dialog.close();
        });

        cancelButton.setOnAction(e -> {
            userChoice.set(Result.CANCEL);
            dialog.close();
        });

        // Handle window close button (X) as cancel
        dialog.setOnCloseRequest(e -> {
            userChoice.set(Result.CANCEL);
        });

        buttons.getChildren().addAll(cancelButton, dontSaveButton, saveButton);

        layout.getChildren().addAll(title, message, buttons);

        Scene scene = new Scene(layout);
        dialog.setScene(scene);

        // Center the dialog on the owner window
        if (owner != null) {
            dialog.setX(owner.getX() + (owner.getWidth() - 350) / 2);
            dialog.setY(owner.getY() + (owner.getHeight() - 150) / 2);
        }

        dialog.showAndWait();

        return userChoice.get();
    }

    private static Button styledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: #1E1E2E; -fx-font-weight: bold; " +
                "-fx-background-radius: 4px; -fx-padding: 6px 12px; " +
                "-fx-font-size: 12px;");

        // Create proper hover effects
        String baseStyle = button.getStyle();
        button.setOnMouseEntered(e -> button.setStyle(baseStyle + "-fx-opacity: 0.85;"));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));

        return button;
    }
}