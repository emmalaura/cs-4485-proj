// Written by: Emma Gonzalez
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
import databaseConnections.GeneratedSentencesQueries;
import java.util.List;

public class SentenceGenController extends BaseController {

    @FXML private VBox rootNode;
    @FXML private Label newChatButton;
    @FXML private VBox chatListBox;
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
    private String currentChatId;

    // This function sets up the chat history functionality and theme
    private void setupChatHistory() {
        if (newChatButton != null) {
            newChatButton.setOnMouseClicked(e -> startNewChat());
        }

        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldText, newText) -> renderChatHistory());
        }

        renderChatHistory();
    }
    // This function starts a new chat and clears the writing area when new chat is clicked
    private void startNewChat() {
        ChatHistoryManager.Chat chat = ChatHistoryManager.createChat(getCurrentPage());
        currentChatId = chat.getId();
        messageArea.getChildren().clear();
        inputField.clear();
        renderChatHistory();
    }
    // This function ensures that a chat is created for the current chatId if it doesn't exist
    private void ensureCurrentChat(String firstMessage) {
        if (currentChatId == null || ChatHistoryManager.getChat(currentChatId) == null) {
            ChatHistoryManager.Chat chat = ChatHistoryManager.createChat(getCurrentPage());
            chat.setTitle(ChatHistoryManager.makeTitle(firstMessage));
            currentChatId = chat.getId();
        }
    }
    // Loads the chat history and renders it to the screen when the page is loaded or search is performed
    private void renderChatHistory() {
        if (chatListBox == null) return;

        chatListBox.getChildren().clear();

        String searchText = searchField == null ? "" : searchField.getText();

        for (ChatHistoryManager.Chat chat : ChatHistoryManager.getChats(getCurrentPage(), searchText)) {
            Label title = new Label(chat.getTitle());
            title.setMaxWidth(Double.MAX_VALUE);
            title.setWrapText(true);
            title.setStyle("-fx-font-size: 14px; -fx-text-fill: " + ThemeManager.getTextColor() + ";");
            // When delete button is clicked, delete chat from chat history tab
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
                    messageArea.getChildren().clear();
                }
                renderChatHistory();
                e.consume();
            });

            chatListBox.getChildren().add(row);
        }
    }
    // This function loads a chat from the chat history and sets up the writing area
    private void loadChat(String chatId) {
        ChatHistoryManager.Chat chat = ChatHistoryManager.getChat(chatId);
        if (chat == null) return;

        currentChatId = chatId;
        messageArea.getChildren().clear();

        for (ChatHistoryManager.ChatMessage message : chat.getMessages()) {
            if (message.isUser()) {
                addUserBubble(message.getText());
            } else {
                addResponseBubble(message.getText());
            }
        }

        renderChatHistory();
    }

    @FXML
    public void initialize() {
        // Load logo and set up theme
        try {
            Image logo = new Image(getClass().getResourceAsStream("autoglossarylogo.png"));
            logoImage.setImage(logo);
        } catch (Exception e) {
            System.out.println("Logo not found, skipping.");
        }
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
    // This function refreshes the theme and applies the appropriate styles to the UI elements based on the current theme settings
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
        // Handle sending user input to the model and displaying the response in the chat window
        String input = inputField.getText().trim();
        if (input.isEmpty()) return;

        ensureCurrentChat(input);
        addUserBubble(input);
        ChatHistoryManager.Chat currentChat = ChatHistoryManager.getChat(currentChatId);
        if(currentChat != null){
            currentChat.addMessage(true, input);
        }
        renderChatHistory();
        inputField.clear();
        // If model is not ready, show loading message to give user confirmation it is loading
        if (!modelReady) {
            String response = "Model is still loading, please wait...";
            addResponseBubble(response);
            if (currentChat != null) {
                currentChat.addMessage(false, response);
                renderChatHistory();
            }
            return;
        }

        // Clean input and split into tokens
        String cleaned = input.toLowerCase().trim();
        String[] tokens = cleaned.split("\\s+");

        if (tokens.length == 0 || tokens[0].isEmpty()) {
            String response = "Please enter a valid word or phrase.";
            addResponseBubble(response);
            if (currentChat != null) {
                currentChat.addMessage(false, response);
                renderChatHistory();
            }
            return;
        }

        // Use last word as seed
        String seed = tokens[tokens.length - 1];

        new Thread(() -> {
            try {
                // Generate a sentence using the model with beam search
                List<String> words = ModelPredictor.completeSentence(
                        model,
                        cleaned,
                        10,
                        GenerationStrategy.BEAM
                );

                String sentence = String.join(" ", words);

                // Insert the generated sentence into the database
                GeneratedSentencesQueries.insertGeneratedSentence(sentence, null, "BEAM", words.size());

                javafx.application.Platform.runLater(() -> {
                    addResponseBubble(sentence);
                    ChatHistoryManager.Chat chat = ChatHistoryManager.getChat(currentChatId);
                    if (chat != null) {
                        chat.addMessage(false, sentence);
                    }
                    renderChatHistory();
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    // Handle any errors that occur during sentence generation and display them to the user
                    String response = "Error generating sentence: " + e.getMessage();
                    addResponseBubble(response);
                    ChatHistoryManager.Chat chat = ChatHistoryManager.getChat(currentChatId);
                    if (chat != null) {
                        chat.addMessage(false, response);
                    }
                    renderChatHistory();
                });
            }
        }).start();
    }

    private void addUserBubble(String text) {
        // Add a user bubble to the chat window with the user's input
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
        // Add a response bubble to the chat window with the model's response
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