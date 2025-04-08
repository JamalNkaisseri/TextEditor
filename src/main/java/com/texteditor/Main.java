package com.texteditor;

import javafx.application.Application;
import javafx.stage.Stage;
import com.texteditor.ui.TextEditorWindow;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        new TextEditorWindow().start(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
