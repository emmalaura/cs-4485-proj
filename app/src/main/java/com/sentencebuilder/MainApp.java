// Written by: Emma Gonzalez
package com.sentencebuilder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Load the main FXML file and show the scene containing the root node
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/sentencebuilder/Home.fxml")
        );
        Scene scene = new Scene(loader.load());
        stage.setTitle("Auto Glossary");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}