package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;

public class AutoCompleteController {

    @FXML private ImageView logoImage;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private TextField searchField;
    @FXML private TextArea writingArea;
    @FXML private HBox suggestionsBar;

    @FXML
    public void initialize() {
        // Load logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }

        // Listen for typing and trigger suggestions after space or comma
        writingArea.textProperty().addListener((obs, oldText, newText) -> {
            if (newText.endsWith(" ") || newText.endsWith(",")) {
                showSuggestions(newText);
            }
        });
    }

    private void showSuggestions(String text) {
        suggestionsBar.getChildren().clear();

        // Placeholder suggestions — backend will replace this
        String[] suggestions = {"example", "technical", "model"};

        for (String suggestion : suggestions) {
            Button btn = new Button(suggestion);
            btn.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                    "-fx-padding: 6 14 6 14; -fx-font-size: 13px; -fx-cursor: hand;");
            btn.setOnAction(e -> insertSuggestion(suggestion));
            suggestionsBar.getChildren().add(btn);
        }
    }

    private void insertSuggestion(String suggestion) {
        String current = writingArea.getText();
        writingArea.setText(current + suggestion + " ");
        writingArea.positionCaret(writingArea.getText().length());
        suggestionsBar.getChildren().clear();
    }

    private void navigateTo(String fxmlPath, javafx.scene.Node source) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(new Scene(loader.load()));
    }


    @FXML
    private void handleSentenceGen() throws Exception {
        navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen);
    }

    @FXML
    private void handleReports() throws Exception {
        navigateTo("/com/sentencebuilder/Reports.fxml", navReports);
    }

    @FXML
    private void handleImport() throws Exception {
        navigateTo("/com/sentencebuilder/Import.fxml", navImport);
    }

    @FXML private void handleHome() throws Exception {
        navigateTo("/com/sentencebuilder/Home.fxml", navHome);
    }
}