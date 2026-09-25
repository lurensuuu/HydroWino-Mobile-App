package com.example.hydrowino;

public class GreenhouseInfo {
    public String name;
    public String location;
    public String dateCreated;

    public GreenhouseInfo() {
        // Required for Firebase
    }

    public GreenhouseInfo(String name, String location, String dateCreated) {
        this.name = name;
        this.location = location;
        this.dateCreated = dateCreated;
    }
}
