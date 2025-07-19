package com.texteditor;

import com.texteditor.ui.TextEditorWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        // Create TextEditorWindow with HostServices to enable link clicking
        TextEditorWindow editor = new TextEditorWindow(getHostServices());
        editor.start(stage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}