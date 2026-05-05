// Written by: Emma Gonzalez
package com.sentencebuilder;

import databaseConnections.WordQueries;
import databaseConnections.GeneratedSentencesQueries;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.*;

public class ReportsController extends BaseController {

    @FXML private VBox rootNode;
    @FXML private HBox navBar;
    @FXML private Button themeBtn;
    @FXML private ImageView logoImage;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private VBox wordListContainer;
    @FXML private VBox sentenceListContainer;
    @FXML private ComboBox<String> sortSelector;
    @FXML private VBox wordListCard;
    @FXML private VBox sentenceCard;
    @FXML private Label reportsTitle;
    @FXML private Label wordListTitle;
    @FXML private Label sentenceTitle;

    private static List<WordEntry> wordEntries = new ArrayList<>();


    private static List<SentenceEntry> sentenceEntries = new ArrayList<>();

    @FXML
    public void initialize() {
        // Load logo and set up theme
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }

        UIUtils.applyCardShadow(wordListCard);
        UIUtils.applyCardShadow(sentenceCard);
        UIUtils.updateLogo(logoImage);
        // Set up sort selector with options and default value based on user preference
        sortSelector.setItems(FXCollections.observableArrayList(
                "Alphabetical (A-Z)", "Frequency (Most Used)", "Date Added"
        ));
        sortSelector.setValue("Alphabetical (A-Z)");
        sortSelector.setOnAction(e -> renderWordList(sortSelector.getValue()));

        new Thread(() -> {
            // Load words and sentences from database in background thread
            if (wordEntries.isEmpty()) {
                loadWordsFromDB();
            }
            loadSentencesFromDB();
            javafx.application.Platform.runLater(() -> {
                renderWordList(sortSelector.getValue());
                renderSentenceList();
            });
        }).start();

        initBase();
        refreshTheme();
        // Set up theme button action to toggle theme and refresh UI
        if (themeBtn != null) {
            themeBtn.setOnAction(e -> {
                ThemeManager.toggleTheme();
                UIUtils.updateLogo(logoImage);
                refreshTheme();
                renderWordList(sortSelector.getValue());
                renderSentenceList();
            });
        }
    }
    // Written by: Kevin
    // This function loads the words from the database and populates the word list
    private void loadWordsFromDB() {
        wordEntries.clear();
        List<WordQueries.WordRecord> records = WordQueries.getAllWords(false);
        for (WordQueries.WordRecord record : records) {
            String sourceFile = WordQueries.getSourceFileForWord(record.wordId);
            String dateAdded = WordQueries.getDateAddedForWord(record.wordId);
            wordEntries.add(new WordEntry(
                    record.word,
                    record.totalOccurrence,
                    dateAdded,
                    sourceFile
            ));
        }
    }
    // Written by: Sajid
    // This function loads the sentences from the database and populates the sentence list
    private void loadSentencesFromDB() {
        sentenceEntries.clear();
        List<GeneratedSentencesQueries.SentenceRecord> records = GeneratedSentencesQueries.getAllGeneratedSentenceRecords();
        for (GeneratedSentencesQueries.SentenceRecord record : records) {
            sentenceEntries.add(new SentenceEntry(record.sentenceText, record.createdAt != null ? record.createdAt : "N/A"));
        }
    }
// Written by: Emma
    // Refreshes the theme and UI elements based on the current theme settings
    private void refreshTheme() {
        getRootNode().setStyle("-fx-background-color: " + ThemeManager.getBackground() + ";");

        navBar.setStyle("-fx-background-color: " + ThemeManager.getNavColor() +
                "; -fx-padding: 16 40 16 20;");

        javafx.application.Platform.runLater(() -> {
            UIUtils.applyNavStyle(navHome, false);
            UIUtils.applyNavStyle(navSentenceGen, false);
            UIUtils.applyNavStyle(navAutoComplete, false);
            UIUtils.applyNavStyle(navReports, true);
            UIUtils.applyNavStyle(navImport, false);
        });

        if (themeBtn != null) {
            themeBtn.setText(ThemeManager.isDark() ? "☀ Light" : "🌙 Dark");
            themeBtn.setStyle("-fx-background-color: transparent; " +
                    "-fx-text-fill: " + ThemeManager.getTextColor() + "; " +
                    "-fx-font-size: 13px; -fx-cursor: hand; " +
                    "-fx-border-color: " + ThemeManager.getTextColor() + "; " +
                    "-fx-border-radius: 20; -fx-background-radius: 20; " +
                    "-fx-padding: 4 12 4 12;");
        }

        sortSelector.setStyle("-fx-background-color: " + ThemeManager.getInputColor() + ";" +
                "-fx-text-fill: " + ThemeManager.getTextColor() + ";" +
                "-fx-border-color: " + ThemeManager.getBorderColor() + ";" +
                "-fx-background-radius: 8;");

        String textColor = ThemeManager.getTextColor();

        if (reportsTitle != null) {
            reportsTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        }

        if (wordListTitle != null) {
            wordListTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        }

        if (sentenceTitle != null) {
            sentenceTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + textColor + ";");
        }
    }

    @Override
    protected Region getRootNode() { return rootNode; }

    @Override
    protected String getCurrentPage() { return "Reports"; }
    // Separate functions for rendering word list and sentence list based on sort option: Alphabetical (A-Z), Frequency (Most Used), Date Added
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
            row.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: " + ThemeManager.getBorderColor() + "; -fx-border-width: 0 0 1 0;");

            Label wordLabel = new Label(entry.word);
            wordLabel.setPrefWidth(250);
            wordLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + ThemeManager.getTextColor() + ";");

            Label freqLabel = new Label(entry.frequency + "x");
            freqLabel.setPrefWidth(150);
            freqLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getSubText() + ";");

            Label dateLabel = new Label(entry.dateAdded);
            dateLabel.setPrefWidth(180);
            dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getSubText() + ";");

            Label sourceLabel = new Label(entry.sourceFile);
            sourceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getSubText() + ";");

            row.getChildren().addAll(wordLabel, freqLabel, dateLabel, sourceLabel);
            wordListContainer.getChildren().add(row);
        }
    }
    private void renderSentenceList() {
        // Group sentences by date and count duplicates for each date
        sentenceListContainer.getChildren().clear();
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
            row.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: " + ThemeManager.getBorderColor() + "; -fx-border-width: 0 0 1 0;");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label sentenceLabel = new Label(sentence);
            sentenceLabel.setWrapText(true);
            sentenceLabel.setMaxWidth(700);
            sentenceLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
            HBox.setHgrow(sentenceLabel, Priority.ALWAYS);

            Label dateLabel = new Label(dateMap.get(sentence));
            dateLabel.setPrefWidth(120);
            dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + ThemeManager.getSubText() + ";");
            // If duplicates exist, add badge with count
            if (count > 1) {
                Label badge = new Label(count + " duplicates");
                badge.setStyle("-fx-background-color: #e53935; -fx-text-fill: white; " +
                        "-fx-background-radius: 20; -fx-padding: 4 10 4 10; -fx-font-size: 12px;");
                row.getChildren().addAll(sentenceLabel, dateLabel, badge);
            } else {
                row.getChildren().addAll(sentenceLabel, dateLabel);
            }
            sentenceListContainer.getChildren().add(row);
        }
    }

    static class WordEntry {
        String word, dateAdded, sourceFile;
        int frequency;
        WordEntry(String word, int frequency, String dateAdded, String sourceFile) {
            this.word = word; this.frequency = frequency;
            this.dateAdded = dateAdded; this.sourceFile = sourceFile;
        }
    }

    static class SentenceEntry {
        String sentence, date;
        SentenceEntry(String sentence, String date) {
            this.sentence = sentence; this.date = date;
        }
    }

    @FXML private void handleHome() { navigateTo("/com/sentencebuilder/Home.fxml", navHome); }
    @FXML private void handleSentenceGen() { navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen); }
    @FXML private void handleAutoComplete() { navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete); }
    @FXML private void handleImport() { navigateTo("/com/sentencebuilder/Import.fxml", navImport); }
}