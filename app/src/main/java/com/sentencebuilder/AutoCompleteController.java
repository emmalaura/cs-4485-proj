package com.sentencebuilder;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.cs4485.model.DBInterface;
import com.cs4485.model.ModelPredictor;
import com.cs4485.model.Prediction;
import databaseConnections.WordQueries;
import java.util.List;
import javafx.geometry.Pos;

public class AutoCompleteController extends BaseController {

    @FXML private VBox rootNode;
    @FXML private HBox navBar;
    @FXML private Button themeBtn;
    @FXML private ImageView logoImage;
    @FXML private Label navHome;
    @FXML private Label navSentenceGen;
    @FXML private Label navAutoComplete;
    @FXML private Label navReports;
    @FXML private Label navImport;
    @FXML private TextArea writingArea;
    @FXML private HBox suggestionsBar;
    @FXML private TextField searchField;
    @FXML private Label newChatButton;
    @FXML private VBox chatListBox;

    private DBInterface db;
    private String currentChatId;
    private boolean loadingChat = false;


    private void setupChatHistory() {
        if (newChatButton != null) {
            newChatButton.setOnMouseClicked(e -> startNewChat());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldText, newText) -> renderChatHistory());
        }

        renderChatHistory();
    }

    private void startNewChat() {
        ChatHistoryManager.Chat chat = ChatHistoryManager.createChat(getCurrentPage());
        currentChatId = chat.getId();

        loadingChat = true;
        writingArea.clear();
        loadingChat = false;

        suggestionsBar.getChildren().clear();
        renderChatHistory();
    }

    private void saveWritingChat(String text) {
        if (loadingChat) return;

        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) return;

        if (currentChatId == null || ChatHistoryManager.getChat(currentChatId) == null) {
            ChatHistoryManager.Chat chat = ChatHistoryManager.createChat(getCurrentPage());
            currentChatId = chat.getId();
        }

        ChatHistoryManager.Chat chat = ChatHistoryManager.getChat(currentChatId);
        if (chat != null) {
            chat.setTitle(ChatHistoryManager.makeTitle(trimmed));
            chat.setContent(text);
            renderChatHistory();
        }
    }

    private void renderChatHistory() {
        if (chatListBox == null) return;

        chatListBox.getChildren().clear();

        String searchText = searchField == null ? "" : searchField.getText();

        for (ChatHistoryManager.Chat chat : ChatHistoryManager.getChats(getCurrentPage(), searchText)) {
            Label title = new Label(chat.getTitle());
            title.setMaxWidth(Double.MAX_VALUE);
            title.setWrapText(true);
            title.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");

            Button deleteButton = new Button("✕");
            deleteButton.setStyle("-fx-background-color: transparent; " +
                    "-fx-text-fill: " + ThemeManager.getSubText() + "; " +
                    "-fx-font-size: 12px; -fx-cursor: hand;");

            HBox row = new HBox(8, title, deleteButton);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(title, Priority.ALWAYS);

            String background = chat.getId().equals(currentChatId)
                    ? ThemeManager.getCardColor()
                    : "transparent";

            row.setStyle("-fx-background-color: " + background + "; " +
                    "-fx-background-radius: 8; " +
                    "-fx-padding: 10 12 10 12; " +
                    "-fx-cursor: hand;");

            row.setOnMouseClicked(e -> loadChat(chat.getId()));

            deleteButton.setOnAction(e -> {
                ChatHistoryManager.deleteChat(chat.getId());
                if (chat.getId().equals(currentChatId)) {
                    currentChatId = null;

                    loadingChat = true;
                    writingArea.clear();
                    loadingChat = false;

                    suggestionsBar.getChildren().clear();
                }
                renderChatHistory();
                e.consume();
            });

            chatListBox.getChildren().add(row);
        }
    }

    private void loadChat(String chatId) {
        ChatHistoryManager.Chat chat = ChatHistoryManager.getChat(chatId);
        if (chat == null) return;

        currentChatId = chatId;

        loadingChat = true;
        writingArea.setText(chat.getContent());
        writingArea.positionCaret(writingArea.getText().length());
        loadingChat = false;

        suggestionsBar.getChildren().clear();
        renderChatHistory();
    }

    @FXML private void handleHome() { navigateTo("/com/sentencebuilder/Home.fxml", navHome); }
    @FXML private void handleSentenceGen() { navigateTo("/com/sentencebuilder/SentenceGen.fxml", navSentenceGen); }
    @FXML private void handleReports() { navigateTo("/com/sentencebuilder/Reports.fxml", navReports); }
    @FXML private void handleImport() { navigateTo("/com/sentencebuilder/Import.fxml", navImport); }

    @FXML
    public void initialize() {
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }

        // Connect to DB in background so UI doesn't freeze
        new Thread(() -> {
            try {
                db = new DBInterface("localhost", "CS4485DB", "javauser", "cs4485");
                System.out.println("AutoComplete: DB connected.");
            } catch (Exception e) {
                System.err.println("AutoComplete: DB connection failed: " + e.getMessage());
            }
        }).start();

        // Trigger suggestions after every space or comma
        writingArea.textProperty().addListener((obs, oldText, newText) -> {
            saveWritingChat(newText);
            if (newText.endsWith(" ") || newText.endsWith(",")) {
                showSuggestions(newText.trim());
            } else {
                if (newText.length() < oldText.length()) {
                    suggestionsBar.getChildren().clear();
                }
            }
        });

        UIUtils.updateLogo(logoImage);
        initBase();
        refreshTheme();
        setupChatHistory();

        if (themeBtn != null) {
            themeBtn.setOnAction(e -> {
                ThemeManager.toggleTheme();
                UIUtils.updateLogo(logoImage);
                refreshTheme();
                renderChatHistory();
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
            UIUtils.applyNavStyle(navAutoComplete, true);
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
        suggestionsBar.getChildren().forEach(node -> {
            if (node instanceof Button btn) {
                btn.setStyle(suggestionBtnStyle());
            }
        });
    }

    @Override protected Region getRootNode() { return rootNode; }
    @Override protected String getCurrentPage() { return "AutoComplete"; }

    private void showSuggestions(String text) {
        if (db == null) {
            System.out.println("AutoComplete: DB not ready yet.");
            return;
        }
        if (text.isEmpty()) return;

        // Get the last word typed
        String[] words = text.split("\\s+");
        if (words.length == 0) return;
        String lastWord = words[words.length - 1].replaceAll("[^a-zA-Z']", "").trim();
        if (lastWord.isEmpty()) return;

        System.out.println("AutoComplete: fetching suggestions for: " + lastWord);

        // Fetch from DB in background
        new Thread(() -> {
            try {
                List<Prediction> predictions = ModelPredictor.getNextWords(db, lastWord, 4);
                System.out.println("AutoComplete: got " + predictions.size() + " suggestions.");
                if (predictions.isEmpty()) {
                    String normalized = lastWord.toLowerCase().trim();
                    int wordId = WordQueries.getWordId(normalized);
                    javafx.application.Platform.runLater(() -> {
                        suggestionsBar.getChildren().clear();
                        if (wordId == -1) {
                            showAddWordOption(normalized);
                        }
                    });
                } else {
                    javafx.application.Platform.runLater(() -> {
                        suggestionsBar.getChildren().clear();
                        for (Prediction p : predictions) {
                            Button btn = new Button(p.word());
                            btn.setStyle(suggestionBtnStyle());
                            btn.setOnAction(e -> insertSuggestion(p.word()));
                            suggestionsBar.getChildren().add(btn);
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("AutoComplete suggestion error: " + e.getMessage());
            }
        }).start();
    }

    private void showAddWordOption(String word) {
        Label info = new Label("\"" + word + "\" not found");
        info.setStyle("-fx-font-size: 13px; -fx-padding: 6 8 6 0; " +
                "-fx-text-fill: " + ThemeManager.getSubText() + ";");
        Button addBtn = new Button("+ Add to dictionary");
        addBtn.setStyle(suggestionBtnStyle());
        addBtn.setOnAction(e -> handleAddWord(word));
        suggestionsBar.getChildren().addAll(info, addBtn);
    }

    private void handleAddWord(String word) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Add '" + word + "' to the database?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                new Thread(() -> {
                    WordQueries.insertOrUpdateWord(word, false, false);
                    javafx.application.Platform.runLater(() -> {
                        suggestionsBar.getChildren().clear();
                        Label confirmation = new Label("Added \"" + word + "\"");
                        confirmation.setStyle("-fx-font-size: 13px; -fx-padding: 6 8 6 0; " +
                                "-fx-text-fill: " + ThemeManager.getTextColor() + ";");
                        suggestionsBar.getChildren().add(confirmation);
                    });
                }).start();
            }
        });
    }

    private void insertSuggestion(String suggestion) {
        String current = writingArea.getText();
        // Append suggestion after the current text
        if (current.endsWith(" ")) {
            writingArea.setText(current + suggestion + " ");
        } else {
            writingArea.setText(current + " " + suggestion + " ");
        }
        writingArea.positionCaret(writingArea.getText().length());
        suggestionsBar.getChildren().clear();
    }

    private String suggestionBtnStyle() {
        return "-fx-background-color: " + ThemeManager.getCardColor() + "; " +
               "-fx-text-fill: " + ThemeManager.getTextColor() + "; " +
               "-fx-background-radius: 20; " +
               "-fx-border-color: " + ThemeManager.getBorderColor() + "; " +
               "-fx-padding: 6 14 6 14; -fx-font-size: 13px; -fx-cursor: hand;";
    }

}
