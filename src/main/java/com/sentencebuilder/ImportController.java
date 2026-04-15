package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
    }

    private void refreshTheme() {
        getRootNode().setStyle("-fx-background-color: " + ThemeManager.getBackground() + ";");

        navBar.setStyle("-fx-background-color: " + ThemeManager.getNavColor() +
                "; -fx-padding: 16 40 16 20;");

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

    @Override
    protected Region getRootNode() { return rootNode; }

    @Override
    protected String getCurrentPage() { return "Import"; }

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
        long approxWords = fileSize / 5;
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));

        HBox row = new HBox();
        row.setStyle("-fx-padding: 10 0 10 0; -fx-border-color: " + ThemeManager.getBorderColor() + "; -fx-border-width: 0 0 1 0;");

        Label nameLabel = new Label(fileName);
        nameLabel.setPrefWidth(300);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");

        Label wordsLabel = new Label(approxWords + " words");
        wordsLabel.setPrefWidth(150);
        wordsLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");

        Label dateLabel = new Label(date);
        dateLabel.setPrefWidth(150);
        dateLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");

        Label statusLabel = new Label("✓ Imported");
        statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #2e7d32;");

        row.getChildren().addAll(nameLabel, wordsLabel, dateLabel, statusLabel);

        importHistoryList.getChildren().removeIf(
                node -> node instanceof Label && ((Label) node).getText().contains("No files")
        );

        importHistoryList.getChildren().add(row);
    }

    @FXML private void handleHome() { navigateTo("/com/sentencebuilder/Home.fxml", navHome); }
    @FXML private void handleSentenceGen() { navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen); }
    @FXML private void handleAutoComplete() { navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete); }
    @FXML private void handleReports() { navigateTo("/com/sentencebuilder/Reports.fxml", navReports); }
}