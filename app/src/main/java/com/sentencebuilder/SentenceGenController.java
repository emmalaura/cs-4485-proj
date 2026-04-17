package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import com.cs4485.model.BigramModel;
import com.cs4485.model.DBInterface;
import com.cs4485.model.GenerationStrategy;
import com.cs4485.model.ModelPredictor;
import com.cs4485.model.SmoothingMethod;
import java.util.List;

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
    private DBInterface db;
    private BigramModel model;
    private boolean modelReady = false;

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
        // Connect to DB and load model in background thread so UI doesn't freeze
        new Thread(() -> {
            try {
                db = new DBInterface("localhost", "CS4485DB", "javauser", "cs4485");
                model = new BigramModel(SmoothingMethod.KNESER_NEY);
                model.train(db.loadWords(), db.loadTransitions());
                modelReady = true;
                javafx.application.Platform.runLater(() ->
                        addResponseBubble("Model ready! Type a word to generate a sentence."));
            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        addResponseBubble("Could not connect to database: " + e.getMessage()));
            }
        }).start();
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
        String input = inputField.getText().trim();
        if (input.isEmpty()) return;

        addUserBubble(input);
        inputField.clear();

        if (!modelReady) {
            addResponseBubble("Model is still loading, please wait...");
            return;
        }

// ✅ ONLY ONCE
        String cleaned = input.toLowerCase().trim();
        String[] tokens = cleaned.split("\\s+");

        if (tokens.length == 0 || tokens[0].isEmpty()) {
            addResponseBubble("Please enter a valid word or phrase.");
            return;
        }

// use LAST WORD
        String seed = tokens[tokens.length - 1];

        new Thread(() -> {
            try {
                List<String> words = ModelPredictor.completeSentence(
                        model,
                        seed,
                        10,
                        GenerationStrategy.BEAM
                );

                String sentence = cleaned + " " + String.join(" ", words);

                javafx.application.Platform.runLater(() ->
                        addResponseBubble(sentence)
                );

            } catch (Exception e) {
                javafx.application.Platform.runLater(() ->
                        addResponseBubble("Error generating sentence: " + e.getMessage())
                );
            }
        }).start();
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