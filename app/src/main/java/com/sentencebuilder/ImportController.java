package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import dataParsing.BigramCounter;
import databaseConnections.ImportedFilesQueries;
import com.cs4485.model.DBInterface;
import com.cs4485.model.BigramModel;
import com.cs4485.model.SmoothingMethod;

public class ImportController extends BaseController {

    @FXML private VBox rootNode;
    @FXML private HBox navBar;
    @FXML private Button themeBtn;
    @FXML private ImageView logoImage;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private VBox importHistoryList;
    @FXML private Label dropLabel;
    @FXML private VBox importCard;
    @FXML private VBox historyCard;

    @FXML
    public void initialize() {
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }
        UIUtils.applyCardShadow(importCard);
        UIUtils.applyCardShadow(historyCard);
        UIUtils.updateLogo(logoImage);
        initBase();
        refreshTheme();
        if (themeBtn != null) {
            themeBtn.setOnAction(e -> {
                ThemeManager.toggleTheme();
                UIUtils.updateLogo(logoImage);
                refreshTheme();
            });
        }
        loadImportHistory();
    }

    private void loadImportHistory() {
        new Thread(() -> {
            java.util.List<String> files = ImportedFilesQueries.getAllImportedFiles();
            javafx.application.Platform.runLater(() -> {
                importHistoryList.getChildren().clear();
                if (files.isEmpty()) {
                    Label empty = new Label("No files imported yet.");
                    empty.setStyle("-fx-text-fill: " + ThemeManager.getSubText() + "; -fx-font-size: 14px;");
                    importHistoryList.getChildren().add(empty);
                } else {
                    for (String fileName : files) {
                        addHistoryRow(fileName, "—",
                            LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                            "✓ Imported");
                    }
                }
            });
        }).start();
    }

    private void refreshTheme() {
        getRootNode().setStyle("-fx-background-color: " + ThemeManager.getBackground() + ";");
        navBar.setStyle("-fx-background-color: " + ThemeManager.getNavColor() + "; -fx-padding: 16 40 16 20;");
        javafx.application.Platform.runLater(() -> {
            UIUtils.applyNavStyle(navHome, false);
            UIUtils.applyNavStyle(navSentenceGen, false);
            UIUtils.applyNavStyle(navAutoComplete, false);
            UIUtils.applyNavStyle(navReports, false);
            UIUtils.applyNavStyle(navImport, true);
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
        if (dropLabel != null) {
            dropLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
        }
    }

    @Override protected Region getRootNode() { return rootNode; }
    @Override protected String getCurrentPage() { return "Import"; }

    @FXML
    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a Text File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        Stage stage = (Stage) dropLabel.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) return;

        dropLabel.setText("Processing " + file.getName() + "...");

        new Thread(() -> {
            try {
                Path filePath = file.toPath();
                byte[] raw = java.nio.file.Files.readAllBytes(filePath);

                // Compute checksum
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hashBytes = md.digest(raw);
                StringBuilder sb = new StringBuilder();
                for (byte b : hashBytes) sb.append(String.format("%02x", b));
                String checksum = sb.toString();

                // Duplicate check
                if (ImportedFilesQueries.checkIfFileImported(checksum)) {
                    javafx.application.Platform.runLater(() ->
                        dropLabel.setText("Already imported: " + file.getName()));
                    return;
                }

                // Tokenize everything first (no DB calls yet)
                String text = new String(raw, java.nio.charset.StandardCharsets.UTF_8);
                String[] sentenceChunks = text.split("(?<=[.!?])\\s+");
                int sentenceCount = sentenceChunks.length;
                int totalWords = 0;
                java.util.Set<String> uniqueWordSet = new java.util.HashSet<>();
                java.util.List<String[]> allSentenceWords = new java.util.ArrayList<>();

                System.out.println("Tokenizing " + sentenceCount + " sentences...");
                for (String chunk : sentenceChunks) {
                    java.util.List<String> words = BigramCounter.tokenize(chunk);
                    if (!words.isEmpty()) {
                        totalWords += words.size();
                        uniqueWordSet.addAll(words);
                        allSentenceWords.add(words.toArray(new String[0]));
                    }
                }
                System.out.println("Tokenized. Words: " + totalWords + ", Unique: " + uniqueWordSet.size());

                // Write everything in one connection with batch inserts
                try (java.sql.Connection conn = databaseConnections.dbConnection.getConnection()) {
                    conn.setAutoCommit(false);

                    // Batch insert/update all words
                    String wordSql = "INSERT INTO words (word, totalOccurrence, startCount, endCount) " +
                                     "VALUES (?, 1, ?, ?) ON DUPLICATE KEY UPDATE " +
                                     "totalOccurrence = totalOccurrence + 1, " +
                                     "startCount = startCount + VALUES(startCount), " +
                                     "endCount = endCount + VALUES(endCount)";
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(wordSql)) {
                        for (String[] words : allSentenceWords) {
                            for (int i = 0; i < words.length; i++) {
                                ps.setString(1, words[i]);
                                ps.setInt(2, i == 0 ? 1 : 0);
                                ps.setInt(3, i == words.length - 1 ? 1 : 0);
                                ps.addBatch();
                            }
                        }
                        ps.executeBatch();
                    }
                    conn.commit();
                    System.out.println("Words committed.");

                    // Load word->id map in one query
                    java.util.Map<String, Integer> wordIdMap = new java.util.HashMap<>();
                    try (java.sql.Statement st = conn.createStatement();
                         java.sql.ResultSet rs = st.executeQuery("SELECT wordId, word FROM words")) {
                        while (rs.next()) wordIdMap.put(rs.getString("word"), rs.getInt("wordId"));
                    }
                    System.out.println("Word ID map loaded: " + wordIdMap.size() + " entries.");

                    // Batch insert/update all transitions
                    String transSql = "INSERT INTO word_transitions (firstWordId, secondWordId, count, probability) " +
                                      "VALUES (?, ?, 1, 0.0) ON DUPLICATE KEY UPDATE count = count + 1";
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(transSql)) {
                        for (String[] words : allSentenceWords) {
                            for (int i = 0; i < words.length - 1; i++) {
                                Integer w1 = wordIdMap.get(words[i]);
                                Integer w2 = wordIdMap.get(words[i + 1]);
                                if (w1 != null && w2 != null) {
                                    ps.setInt(1, w1);
                                    ps.setInt(2, w2);
                                    ps.addBatch();
                                }
                            }
                        }
                        ps.executeBatch();
                    }
                    conn.commit();
                    System.out.println("Transitions committed.");
                }

                // Record file in imported_files table
                final int fw = totalWords;
                final int fs = sentenceCount;
                final int fu = uniqueWordSet.size();
                ImportedFilesQueries.insertImportedFile(
                    file.getName(), file.getAbsolutePath(),
                    file.length(), fw, fs, fu, checksum);

                // Retrain model and write probabilities back to DB
                System.out.println("Retraining model...");
                try (DBInterface modelDb = new DBInterface("localhost", "CS4485DB", "javauser", "cs4485")) {
                    BigramModel model = new BigramModel(SmoothingMethod.KNESER_NEY);
                    model.train(modelDb.loadWords(), modelDb.loadTransitions());
                    Map<int[], Double> probs = model.computeAllProbabilities();
                    modelDb.updateProbabilities(probs);
                    System.out.println("Done. Probabilities updated: " + probs.size());
                }

                javafx.application.Platform.runLater(() -> {
                    dropLabel.setText("Drop a .txt file here or click to browse");
                    importHistoryList.getChildren().removeIf(node ->
                        node instanceof Label && ((Label) node).getText().contains("No files"));
                    addHistoryRow(file.getName(), fw + " words",
                        LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                        "✓ Imported");
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() ->
                    dropLabel.setText("Error: " + e.getMessage()));
            }
        }).start();
    }

    private void addHistoryRow(String fileName, String words, String date, String status) {
        HBox row = new HBox();
        row.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: " + ThemeManager.getBorderColor() + "; -fx-border-width: 0 0 1 0;");
        Label nameLabel = new Label(fileName);
        nameLabel.setPrefWidth(300);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
        Label wordsLabel = new Label(words);
        wordsLabel.setPrefWidth(150);
        wordsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
        Label dateLabel = new Label(date);
        dateLabel.setPrefWidth(150);
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
        Label statusLabel = new Label(status);
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2e7d32;");
        row.getChildren().addAll(nameLabel, wordsLabel, dateLabel, statusLabel);
        importHistoryList.getChildren().add(row);
    }

    @FXML private void handleHome() { navigateTo("/com/sentencebuilder/Home.fxml", navHome); }
    @FXML private void handleSentenceGen() { navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen); }
    @FXML private void handleAutoComplete() { navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete); }
    @FXML private void handleReports() { navigateTo("/com/sentencebuilder/Reports.fxml", navReports); }
}
