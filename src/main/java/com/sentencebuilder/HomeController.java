package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class HomeController extends BaseController {

    @FXML private VBox rootNode;
    @FXML private HBox navBar;
    @FXML private ImageView logoImage;
    @FXML private Button generateBtn;
    @FXML private Button themeBtn;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private Label welcomeTitle;
    @FXML private Label welcomeSubtitle;

    @FXML
    public void initialize() {
        // Load logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }
        UIUtils.updateLogo(logoImage);
        // Button style
        UIUtils.applyDarkButtonStyle(generateBtn);

        // Apply theme on load
        initBase();
        refreshTheme();

        // Wire theme button
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
            UIUtils.applyNavStyle(navHome, true);
            UIUtils.applyNavStyle(navSentenceGen, false);
            UIUtils.applyNavStyle(navAutoComplete, false);
            UIUtils.applyNavStyle(navReports, false);
            UIUtils.applyNavStyle(navImport, false);
        });

        welcomeTitle.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                + ThemeManager.getTextColor() + ";");
        welcomeSubtitle.setStyle("-fx-font-size: 18px; -fx-text-fill: "
                + ThemeManager.getSubText() + "; -fx-text-alignment: center;");

        if (themeBtn != null) {
            themeBtn.setText(ThemeManager.isDark() ? "☀ Light" : "🌙 Dark");
            themeBtn.setStyle("-fx-background-color: transparent; " +
                    "-fx-text-fill: " + ThemeManager.getTextColor() + "; " +
                    "-fx-font-size: 13px; -fx-cursor: hand; " +
                    "-fx-border-color: " + ThemeManager.getTextColor() + "; " +
                    "-fx-border-radius: 20; -fx-background-radius: 20; " +
                    "-fx-padding: 4 12 4 12;");
        }
    }

    @Override
    protected Region getRootNode() { return rootNode; }

    @Override
    protected String getCurrentPage() { return "Home"; }

    @FXML private void handleGenerate() { navigateTo("/com/sentencebuilder/SentenceGen.fxml", generateBtn); }
    @FXML private void handleSentenceGen() { navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen); }
    @FXML private void handleAutoComplete() { navigateTo("/com/sentencebuilder/AutoComplete.fxml", navAutoComplete); }
    @FXML private void handleReports() { navigateTo("/com/sentencebuilder/Reports.fxml", navReports); }
    @FXML private void handleImport() { navigateTo("/com/sentencebuilder/Import.fxml", navImport); }
    @FXML private void handleHome() { navigateTo("/com/sentencebuilder/Home.fxml", navSentenceGen); }
}