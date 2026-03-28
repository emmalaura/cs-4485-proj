package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportsController {

    @FXML private ImageView logoImage;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private VBox wordListContainer;
    @FXML private VBox sentenceListContainer;
    @FXML private ComboBox<String> sortSelector;

    // Sample data — will be replaced by backend data later
    private final List<WordEntry> wordEntries = new ArrayList<>(Arrays.asList(
            new WordEntry("bigram", 12, "2026-03-01", "textbook.txt"),
            new WordEntry("algorithm", 9, "2026-03-02", "manual.txt"),
            new WordEntry("markov", 7, "2026-03-01", "textbook.txt"),
            new WordEntry("language", 15, "2026-03-03", "textbook.txt"),
            new WordEntry("model", 11, "2026-03-02", "manual.txt"),
            new WordEntry("token", 5, "2026-03-04", "manual.txt"),
            new WordEntry("probability", 8, "2026-03-03", "textbook.txt")
    ));

    private final List<SentenceEntry> sentenceEntries = new ArrayList<>(Arrays.asList(
            new SentenceEntry("A bigram language model predicts the next token.", "2026-03-10"),
            new SentenceEntry("The algorithm uses probability to generate text.", "2026-03-11"),
            new SentenceEntry("A bigram language model predicts the next token.", "2026-03-12"),
            new SentenceEntry("Markov chain model simplifies language modeling.", "2026-03-13"),
            new SentenceEntry("The algorithm uses probability to generate text.", "2026-03-14"),
            new SentenceEntry("Token probability defines the markov model.", "2026-03-15")
    ));

    @FXML
    public void initialize() {
        // Load logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }

        // Set up sort options
        sortSelector.setItems(FXCollections.observableArrayList(
                "Alphabetical (A-Z)", "Frequency (Most Used)", "Date Added"
        ));
        sortSelector.setValue("Alphabetical (A-Z)");
        sortSelector.setOnAction(e -> renderWordList(sortSelector.getValue()));

        renderWordList("Alphabetical (A-Z)");
        renderSentenceList();
    }

    private void renderWordList(String sortBy) {
        wordListContainer.getChildren().clear();

        List<WordEntry> sorted = new ArrayList<>(wordEntries);
        switch (sortBy) {
            case "Frequency (Most Used)" -> sorted.sort((a, b) -> b.frequency - a.frequency);
            case "Date Added" -> sorted.sort(Comparator.comparing(a -> a.dateAdded));
            default -> sorted.sort(Comparator.comparing(a -> a.word));
        }

        for (WordEntry entry : sorted) {
            HBox row = new HBox();
            row.setStyle("-fx-padding: 10 0 10 0; " +
                    "-fx-border-color: #eeeeee; " +
                    "-fx-border-width: 0 0 1 0;");

            Label wordLabel = new Label(entry.word);
            wordLabel.setPrefWidth(250);
            wordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            Label freqLabel = new Label(entry.frequency + "x");
            freqLabel.setPrefWidth(150);
            freqLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");

            Label dateLabel = new Label(entry.dateAdded);
            dateLabel.setPrefWidth(180);
            dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");

            Label sourceLabel = new Label(entry.sourceFile);
            sourceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555;");

            row.getChildren().addAll(wordLabel, freqLabel, dateLabel, sourceLabel);
            wordListContainer.getChildren().add(row);
        }
    }

    private void renderSentenceList() {
        sentenceListContainer.getChildren().clear();

        // Count duplicates
        Map<String, Integer> countMap = new LinkedHashMap<>();
        Map<String, String> dateMap = new LinkedHashMap<>();
        for (SentenceEntry entry : sentenceEntries) {
            countMap.put(entry.sentence, countMap.getOrDefault(entry.sentence, 0) + 1);
            dateMap.put(entry.sentence, entry.date);
        }

        for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
            String sentence = entry.getKey();
            int count = entry.getValue();

            HBox row = new HBox(12);
            row.setStyle("-fx-padding: 10 0 10 0; " +
                    "-fx-border-color: #eeeeee; " +
                    "-fx-border-width: 0 0 1 0;");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label sentenceLabel = new Label(sentence);
            sentenceLabel.setWrapText(true);
            sentenceLabel.setMaxWidth(700);
            sentenceLabel.setStyle("-fx-font-size: 14px;");
            HBox.setHgrow(sentenceLabel, Priority.ALWAYS);

            Label dateLabel = new Label(dateMap.get(sentence));
            dateLabel.setPrefWidth(120);
            dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #888888;");

            // Duplicate badge
            if (count > 1) {
                Label badge = new Label(count + " duplicates");
                badge.setStyle("-fx-background-color: #e53935; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 4 10 4 10; " +
                        "-fx-font-size: 12px;");
                row.getChildren().addAll(sentenceLabel, dateLabel, badge);
            } else {
                row.getChildren().addAll(sentenceLabel, dateLabel);
            }

            sentenceListContainer.getChildren().add(row);
        }
    }

    // --- Data classes ---
    static class WordEntry {
        String word, dateAdded, sourceFile;
        int frequency;
        WordEntry(String word, int frequency, String dateAdded, String sourceFile) {
            this.word = word;
            this.frequency = frequency;
            this.dateAdded = dateAdded;
            this.sourceFile = sourceFile;
        }
    }

    static class SentenceEntry {
        String sentence, date;
        SentenceEntry(String sentence, String date) {
            this.sentence = sentence;
            this.date = date;
        }
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

    @FXML private void handleImport() throws Exception {
        navigateTo("/com/sentencebuilder/Import.fxml", navImport);
    }

    @FXML
    private void handleReports() throws Exception{
        navigateTo("/com/sentencebuilder/Reports.fxml", navReports);
    }
}