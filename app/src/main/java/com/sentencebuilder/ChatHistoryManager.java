// Written by: Emma Gonzalez
package com.sentencebuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ChatHistoryManager {

    public static class ChatMessage {
        private final boolean user;
        private final String text;

        public ChatMessage(boolean user, String text) {
            this.user = user;
            this.text = text;
        }

        public boolean isUser() {
            return user;
        }

        public String getText() {
            return text;
        }
    }

    public static class Chat {
        private final String id;
        private final String page;
        private String title;
        private String content;
        private final List<ChatMessage> messages;
        private long updatedAt;

        private Chat(String page, String title) {
            this.id = UUID.randomUUID().toString();
            this.page = page;
            this.title = title;
            this.content = "";
            this.messages = new ArrayList<>();
            this.updatedAt = System.currentTimeMillis();
        }

        public String getId() {
            return id;
        }

        public String getPage() {
            return page;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public List<ChatMessage> getMessages() {
            return messages;
        }
        // When new chat is created, set default title and content
        public void setTitle(String title) {
            this.title = title;
            touch();
        }
        public void setContent(String content) {
            this.content = content == null ? "" : content;
            touch();
        }
        // When new message is added, update title and content based on message content
        public void addMessage(boolean user, String text) {
            messages.add(new ChatMessage(user, text));
            if ((title == null || title.isBlank() || title.equals("New Chat")) && text != null && !text.isBlank()) {
                title = makeTitle(text);
            }
            content = messages.stream()
                    .map(ChatMessage::getText)
                    .collect(Collectors.joining("\n"));
            touch();
        }

        private void touch() {
            updatedAt = System.currentTimeMillis();
        }
    }

    private static final List<Chat> chats = new ArrayList<>();
    // Create a new chat for the given page, and add it to the list of chats
    public static Chat createChat(String page) {
        Chat chat = new Chat(page, "New Chat");
        chats.add(chat);
        return chat;
    }
    // Get all chats for the given page, sorted by last updated time (newest first)
    public static List<Chat> getChats(String page, String searchText) {
        String query = searchText == null ? "" : searchText.trim().toLowerCase();

        return chats.stream()
                .filter(chat -> chat.getPage().equals(page))
                .filter(chat -> query.isEmpty()
                        || chat.getTitle().toLowerCase().contains(query)
                        || chat.getContent().toLowerCase().contains(query))
                .sorted(Comparator.comparingLong((Chat chat) -> chat.updatedAt).reversed())
                .collect(Collectors.toList());
    }
    // Get a specific chat by its ID
    public static Chat getChat(String id) {
        return chats.stream()
                .filter(chat -> chat.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static void deleteChat(String id) {
        chats.removeIf(chat -> chat.getId().equals(id));
    }

    public static String makeTitle(String text) {
        if (text == null || text.isBlank()) {
            return "New Chat";
        }

        String cleaned = text.trim().replaceAll("\\s+", " ");
        return cleaned.length() > 32 ? cleaned.substring(0, 32) + "..." : cleaned;
    }
}