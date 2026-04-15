package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;

public class SentenceGenController extends BaseController {

    @FXML private VBox rootNode;
    @FXML private HBox navBar;
    @FXML private Button themeBtn;
    @FXML private ImageView logoImage;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private VBox messageArea;
    @FXML private TextField inputField;
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }
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
            UIUtils.applyNavStyle(navSentenceGen, true);
            UIUtils.applyNavStyle(navAutoComplete, false);
            UIUtils.applyNavStyle(navReports, false);
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

        inputField.setStyle("-fx-background-color: " + ThemeManager.getInputColor() + ";" +
                "-fx-control-inner-background: " + ThemeManager.getInputColor() + ";" +
                "-fx-text-fill: " + ThemeManager.getTextColor() + ";" +
                "-fx-prompt-text-fill: " + ThemeManager.getPromptTextColor() + ";" +
                "-fx-background-radius: 8; -fx-border-color: " + ThemeManager.getBorderColor() + ";");

        searchField.setStyle("-fx-background-color: " + ThemeManager.getInputColor() + ";" +
                "-fx-control-inner-background: " + ThemeManager.getInputColor() + ";" +
                "-fx-text-fill: " + ThemeManager.getTextColor() + ";" +
                "-fx-prompt-text-fill: " + ThemeManager.getPromptTextColor() + ";" +
                "-fx-background-radius: 8; -fx-border-color: " + ThemeManager.getBorderColor() + ";");
    }

    @Override
    protected Region getRootNode() { return rootNode; }

    @Override
    protected String getCurrentPage() { return "SentenceGen"; }

    @FXML
    private void handleSend() {
        String word = inputField.getText().trim();
        if (!word.isEmpty()) {
            addUserBubble(word);
            inputField.clear();
            addResponseBubble("...");
        }
    }

    private void addUserBubble(String text) {
        Label bubble = new Label(text);
        bubble.setStyle("-fx-background-color: " + ThemeManager.getCardColor() + "; " +
                "-fx-text-fill: " + ThemeManager.getTextColor() + "; " +
                "-fx-background-radius: 12; " +
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
        bullet.setStyle("-fx-font-size: 16px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
        Label bubble = new Label(text);
        bubble.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
        bubble.setMaxWidth(600);
        bubble.setWrapText(true);
        HBox row = new HBox(10, bullet, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 6 20 6 20;");
        messageArea.getChildren().add(row);
    }

    @FXML private void handleHome() { navigateTo("/com/sentencebuilder/Home.fxml", navHome); }
    @FXML private void handleAutoComplete() { navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete); }
    @FXML private void handleReports() { navigateTo("/com/sentencebuilder/Reports.fxml", navReports); }
    @FXML private void handleImport() { navigateTo("/com/sentencebuilder/Import.fxml", navImport); }
}