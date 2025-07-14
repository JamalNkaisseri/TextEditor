package com.texteditor;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public void start(Stage stage){
        new TextEditorWindow().start(stage);
    }

    public static void main(String[] args){
        launch(args);
    }
}
