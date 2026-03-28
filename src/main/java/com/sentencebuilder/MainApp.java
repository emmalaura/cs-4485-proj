package com.sentencebuilder;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Label label = new Label("Sentence Builder - JavaFX Running!");
        Scene scene = new Scene(new StackPane(label), 640, 480);
        stage.setScene(scene);
        stage.setTitle("Sentence Builder");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}