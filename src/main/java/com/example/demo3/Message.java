package com.example.demo3;

public class Message {
    private String content;      // Conținutul mesajului
    private String timestamp;    // Data și ora la care a fost trimis mesajul

    public Message(String content, String timestamp) {
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getteri și setteri
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
