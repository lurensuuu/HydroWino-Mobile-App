package com.example.hydrowino;

public class Status {
    public String connectivity;
    public long lastPulse;

    public Status() {
        // Required for Firebase
    }

    public Status(String connectivity, long lastPulse) {
        this.connectivity = connectivity;
        this.lastPulse = lastPulse;
    }
}
