// Written by: Emma Gonzalez
package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public abstract class BaseController {

    @FXML protected Button themeBtn;

    // Each controller tells us its root node and current page name
    protected abstract Region getRootNode();
    protected abstract String getCurrentPage();

    protected void initBase() {
        applyTheme();
        // Apply theme button, if clicked toggles theme between light and dark mode
        if (themeBtn != null) {
            themeBtn.setText(ThemeManager.isDark() ? "☀ Light" : "🌙 Dark");
            themeBtn.setOnAction(e -> {
                ThemeManager.toggleTheme();
                applyTheme();
                themeBtn.setText(ThemeManager.isDark() ? "☀ Light" : "🌙 Dark");
            });
        }
    }
    // Applies theme to the root node and all its children, light or dark mode depending on user preference
    protected void applyTheme() {
        Region root = getRootNode();
        if (root == null) return;

        // Background
        root.setStyle("-fx-background-color: " + ThemeManager.getBackground() + ";");

        // All labels
        root.lookupAll("Label").forEach(n -> {
            if (n instanceof Label l) {
                String current = l.getStyle() == null ? "" : l.getStyle();
                if (!current.contains("-fx-text-fill")) {
                    l.setStyle(current + "-fx-text-fill: " + ThemeManager.getTextColor() + ";");
                }
            }
        });
        // Apply theme to text fields
        root.lookupAll("TextField").forEach(n -> {
            if (n instanceof TextField tf) {
                tf.setStyle(
                        "-fx-background-color: " + ThemeManager.getInputColor() + ";" +
                        "-fx-control-inner-background: " + ThemeManager.getInputColor() + ";" +
                        "-fx-text-fill: " + ThemeManager.getTextColor() + ";" +
                        "-fx-prompt-text-fill: " + ThemeManager.getPromptTextColor() + ";" +
                        "-fx-border-color: " + ThemeManager.getBorderColor() + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
                );
            }
        });
        // Apply theme to text areas
        root.lookupAll("TextArea").forEach(n -> {
            if (n instanceof TextArea ta) {
                ta.setStyle(
                        "-fx-background-color: " + ThemeManager.getInputColor() + ";" +
                        "-fx-control-inner-background: " + ThemeManager.getInputColor() + ";" +
                        "-fx-text-fill: " + ThemeManager.getTextColor() + ";" +
                        "-fx-prompt-text-fill: " + ThemeManager.getPromptTextColor() + ";" +
                        "-fx-border-color: " + ThemeManager.getBorderColor() + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;"
                );
            }
        });
        // Apply theme to combo boxes
        root.lookupAll("ComboBox").forEach(n -> {
            if (n instanceof ComboBox<?> cb) {
                cb.setStyle(
                        "-fx-background-color: " + ThemeManager.getInputColor() + ";" +
                        "-fx-text-fill: " + ThemeManager.getTextColor() + ";" +
                        "-fx-border-color: " + ThemeManager.getBorderColor() + ";" +
                        "-fx-background-radius: 8;"
                );
            }
        });
        // Apply theme to cards
        root.lookupAll(".card").forEach(n -> {
            if (n instanceof VBox v) {
                v.setStyle(v.getStyle() +
                        "-fx-background-color: " + ThemeManager.getCardColor() + ";");
            }
        });
    }
    // Navigate to a new page when a button is clicked
    protected void navigateTo(String fxmlPath, javafx.scene.Node source) {
        try {
            var url = getClass().getResource(fxmlPath);
            if (url == null) {
                System.out.println("ERROR: FXML not found: " + fxmlPath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(url);
            Stage stage = (Stage) source.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}