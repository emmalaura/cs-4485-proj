package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;


public class HomeController {

    @FXML private ImageView logoImage;
    @FXML private Button generateBtn;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private Label navHome;

    @FXML
    public void initialize() {
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }
    }

    @FXML
    private void handleGenerate() {
        navigateTo("/com/sentencebuilder/SentenceGen.fxml", generateBtn);
    }

    @FXML
    private void handleSentenceGen() {
        navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen);
    }

    @FXML
    private void handleAutoComplete() {
        navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete);
    }

    @FXML
    private void handleReports() {
        navigateTo("/com/sentencebuilder/Reports.fxml", navReports);
    }

    @FXML
    private void handleImport() {
        navigateTo("/com/sentencebuilder/Import.fxml", navImport);
    }

    @FXML
    private void handleHome() {
        navigateTo("/com/sentencebuilder/Home.fxml", navSentenceGen);
    }

    private void navigateTo(String fxmlPath, javafx.scene.Node source) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("ERROR: FXML not found at path: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            System.out.println("ERROR navigating to " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
