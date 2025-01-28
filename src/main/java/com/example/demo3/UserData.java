package com.example.demo3;

import java.util.List;
import java.util.Map;

public class UserData {
    private String username;
    private Map<String, List<String>> videos; // Categoriile de videoclipuri
    private List<String> messages;           // Mesajele din chat
    private String lastSynced;               // Data ultimei sincronizări

    public UserData(String username, Map<String, List<String>> videos, List<String> messages, String lastSynced) {
        this.username = username;
        this.videos = videos;
        this.messages = messages;
        this.lastSynced = lastSynced;
    }

    // Getteri și setteri
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public Map<String, List<String>> getVideos() { return videos; }
    public void setVideos(Map<String, List<String>> videos) { this.videos = videos; }

    public List<String> getMessages() { return messages; }
    public void setMessages(List<String> messages) { this.messages = messages; }

    public String getLastSynced() { return lastSynced; }
    public void setLastSynced(String lastSynced) { this.lastSynced = lastSynced; }
}
