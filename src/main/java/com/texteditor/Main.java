package com.texteditor;

<<<<<<< HEAD
import com.texteditor.ui.TextEditorWindow;
=======
>>>>>>> 7500b13cdb2c67f29b35110b73620aef84c189b2
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
