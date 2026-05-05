// Written by: Emma Gonzalez
package com.sentencebuilder;

import javafx.scene.Scene;

public class ThemeManager {

    public enum Theme { LIGHT, DARK }

    private static Theme currentTheme = Theme.LIGHT;

    public static Theme getTheme() { return currentTheme; }

    public static void toggleTheme() {
        currentTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
    }

    public static boolean isDark() { return currentTheme == Theme.DARK; }

    // Theme colors, depending on the current theme settings
    public static String getBackground()   { return isDark() ? "#1e1e1e" : "#D9D9D9"; }
    public static String getCardColor()    { return isDark() ? "#2c2c2c" : "#ffffff"; }
    public static String getTextColor()    { return isDark() ? "#ffffff" : "#1a1a1a"; }
    public static String getSubText()      { return isDark() ? "#cccccc" : "#888888"; }
    public static String getSidebarColor() { return isDark() ? "#252525" : "#D9D9D9"; }
    public static String getInputColor()   { return isDark() ? "#3a3a3a" : "#e0e0e0"; }
    public static String getBorderColor()  { return isDark() ? "#444444" : "#bbbbbb"; }
    public static String getSearchColor()  { return isDark() ? "#3a3a3a" : "#ffffff"; }
    public static String getNavColor() { return isDark() ? "#2a2a2a" : "#D9D9D9"; }

    public static String getButtonColor() {
        return isDark() ? "#3a3a3a" : "#2b2b2b";
    }

    public static String getButtonHoverColor() {
        return isDark() ? "#4a4a4a" : "#444444";
    }

    public static String getPromptTextColor() {
        return isDark() ? "#aaaaaa" : "#888888";
    }

    public static void applyBackground(javafx.scene.layout.Region root) {
        root.setStyle("-fx-background-color: " + getBackground() + ";");
    }
}