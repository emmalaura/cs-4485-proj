package com.sentencebuilder;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class UIUtils {

    // ── Nav hover + active indicator ──────────────────────────────────────

    public static void applyNavStyle(Label label, boolean isActive) {
        String textColor = ThemeManager.getTextColor();
        String base = "-fx-font-size: 18px; -fx-cursor: hand; -fx-text-fill: " + textColor +
                "; -fx-padding: 4 0 4 0;";
        String active = "-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + textColor +
                "; -fx-padding: 4 0 4 0;" +
                "-fx-border-color: transparent transparent " + textColor + " transparent; " +
                "-fx-border-width: 0 0 2 0;";

        label.setStyle("");
        label.applyCss();
        label.setStyle(isActive ? active : base);

        if (!isActive) {
            label.setOnMouseEntered(e -> label.setStyle(base +
                    "-fx-border-color: transparent transparent #888888 transparent; " +
                    "-fx-border-width: 0 0 2 0;"));
            label.setOnMouseExited(e -> label.setStyle(base));
        }
    }
    public static void applyDarkButtonStyle(Button button) {
        String normal = "-fx-background-color: " + ThemeManager.getButtonColor() + "; -fx-text-fill: white; " +
                "-fx-font-size: 16px; -fx-padding: 16 40 16 40; " +
                "-fx-background-radius: 12; -fx-cursor: hand;";
        String hover  = "-fx-background-color: " + ThemeManager.getButtonHoverColor() + "; -fx-text-fill: white; " +
                "-fx-font-size: 16px; -fx-padding: 16 40 16 40; " +
                "-fx-background-radius: 12; -fx-cursor: hand;";

        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
    }

    public static void applyLightButtonStyle(Button button) {
        String normal = "-fx-background-color: " + ThemeManager.getButtonColor() + "; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 10 24 10 24; " +
                "-fx-background-radius: 20; -fx-cursor: hand;";
        String hover  = "-fx-background-color: " + ThemeManager.getButtonHoverColor() + "; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-padding: 10 24 10 24; " +
                "-fx-background-radius: 20; -fx-cursor: hand;";

        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
    }

    // ── Drop shadow on cards ──────────────────────────────────────────────

    public static void applyCardShadow(VBox card) {
        card.setStyle(card.getStyle() +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");
    }

    // ── Theme toggle button ───────────────────────────────────────────────

    public static Button createThemeToggleButton(Runnable onToggle) {
        Button btn = new Button(ThemeManager.isDark() ? "☀ Light" : "🌙 Dark");
        btn.setStyle("-fx-background-color: transparent; " +
                "-fx-text-fill: #888888; " +
                "-fx-font-size: 13px; " +
                "-fx-cursor: hand; " +
                "-fx-border-color: #888888; " +
                "-fx-border-radius: 20; " +
                "-fx-background-radius: 20; " +
                "-fx-padding: 4 12 4 12;");
        btn.setOnAction(e -> {
            ThemeManager.toggleTheme();
            btn.setText(ThemeManager.isDark() ? "☀ Light" : "🌙 Dark");
            onToggle.run();
        });
        return btn;
    }
    public static void updateLogo(ImageView logoImage) {
        if (logoImage == null) return;

        String logoPath = ThemeManager.isDark()
                ? "/com/sentencebuilder/darklogo.png"
                : "/com/sentencebuilder/lightlogo.png";

        try {
            Image logo = new Image(UIUtils.class.getResourceAsStream(logoPath));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found: " + logoPath);
        }
    }
}