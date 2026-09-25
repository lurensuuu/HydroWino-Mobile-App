package com.example.hydrowino;

import com.google.firebase.firestore.Exclude;

public class NotificationLog {
    private String id; // Firestore document ID
    private String title;
    private String message;
    private long timestamp;
    private String type;
    private boolean read;

    public NotificationLog() {
        // Required for Firestore
    }

    public NotificationLog(String title, String message, long timestamp, String type, boolean read) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.type = type;
        this.read = read;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
    public String getType() { return type; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}