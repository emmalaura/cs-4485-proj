package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextAlignment;

public class SentenceGenController {

    @FXML private Label navHome;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private VBox messageArea;
    @FXML private TextField inputField;
    @FXML private ComboBox<String> algorithmSelector;
    @FXML private TextField searchField;

    @FXML private ImageView logoImage;

    @FXML
    public void initialize() {
        algorithmSelector.getItems().addAll("Bigram", "Trigram", "Markov Chain");
        algorithmSelector.setPromptText("Select Algorithm");

        // Load logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }
    }

    private void navigateTo(String fxmlPath, javafx.scene.Node source) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(new Scene(loader.load()));
    }

    @FXML
    private void handleHome() throws Exception {
        navigateTo("/com/sentencebuilder/Home.fxml", navHome);
    }

    @FXML
    private void handleImport() throws Exception {
        navigateTo("/com/sentencebuilder/Import.fxml", navImport);
    }

    @FXML
    private void handleAutoComplete() throws Exception {
        navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete);
    }

    @FXML
    private void
    handleReports() throws Exception {
        navigateTo("/com/sentencebuilder/Reports.fxml", navReports);
    }

    private void addUserBubble(String text) {
        Label bubble = new Label(text);
        bubble.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-padding: 10 16 10 16; -fx-font-size: 14px;");
        bubble.setMaxWidth(500);
        bubble.setWrapText(true);

        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setStyle("-fx-padding: 6 20 6 20;");
        messageArea.getChildren().add(row);
    }

    private void addResponseBubble(String text) {
        Label bullet = new Label("⊞");
        bullet.setStyle("-fx-font-size: 16px; -fx-text-fill: #1a1a1a;");

        Label bubble = new Label(text);
        bubble.setStyle("-fx-font-size: 14px; -fx-text-fill: #1a1a1a;");
        bubble.setMaxWidth(600);
        bubble.setWrapText(true);

        HBox row = new HBox(10, bullet, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 6 20 6 20;");
        messageArea.getChildren().add(row);
    }

    @FXML
    private void handleSend() {
        String word = inputField.getText().trim();
        if (!word.isEmpty()) {
            addUserBubble(word);
            inputField.clear();
            addResponseBubble("...");
        }
    }
}