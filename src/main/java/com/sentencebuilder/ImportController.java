package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ImportController {

    @FXML private ImageView logoImage;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private VBox importHistoryList;
    @FXML private Label dropLabel;

    @FXML
    public void initialize() {
        // Load logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }
    }

    @FXML
    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a Text File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        Stage stage = (Stage) dropLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            addToImportHistory(file.getName(), file.length());
        }
    }

    private void addToImportHistory(String fileName, long fileSize) {
        // Calculate approximate word count (rough estimate)
        long approxWords = fileSize / 5;
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

        // Create a row in the history table
        HBox row = new HBox();
        row.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: #eeeeee; -fx-border-width: 0 0 1 0;");

        Label nameLabel = new Label(fileName);
        nameLabel.setPrefWidth(300);
        nameLabel.setStyle("-fx-font-size: 14px;");

        Label wordsLabel = new Label(approxWords + " words");
        wordsLabel.setPrefWidth(150);
        wordsLabel.setStyle("-fx-font-size: 14px;");

        Label dateLabel = new Label(date);
        dateLabel.setPrefWidth(150);
        dateLabel.setStyle("-fx-font-size: 14px;");

        Label statusLabel = new Label("✓ Imported");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2e7d32;");

        row.getChildren().addAll(nameLabel, wordsLabel, dateLabel, statusLabel);

        // Remove "no files" placeholder if present
        importHistoryList.getChildren().removeIf(
                node -> node instanceof Label && ((Label) node).getText().contains("No files")
        );

        importHistoryList.getChildren().add(row);
    }

    private void navigateTo(String fxmlPath, javafx.scene.Node source) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Stage stage = (Stage) source.getScene().getWindow();
        stage.setScene(new Scene(loader.load()));
    }

    @FXML private void handleHome() throws Exception {
        navigateTo("/com/sentencebuilder/Home.fxml", navHome);
    }

    @FXML private void handleSentenceGen() throws Exception {
        navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen);
    }

    @FXML private void handleAutoComplete() throws Exception {
        navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete);
    }

    @FXML
    private void handleReports() throws Exception {
        navigateTo("/com/sentencebuilder/Reports.fxml", navReports);
    }
}