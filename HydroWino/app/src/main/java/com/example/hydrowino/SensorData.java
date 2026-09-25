package com.example.hydrowino;

public class SensorData {
    public float humidity;
    public float ph;
    public float airTemp;
    public float waterLevel;
    public float tds;
    public float waterTemp;
    public long lastUpdated;

    public SensorData() {
        // Required for Firebase
    }

    public SensorData(float humidity, float ph, float airTemp, float waterLevel, float tds, float waterTemp) {
        this.humidity = humidity;
        this.ph = ph;
        this.airTemp = airTemp;
        this.waterLevel = waterLevel;
        this.tds = tds;
        this.waterTemp = waterTemp;
        this.lastUpdated = System.currentTimeMillis();
    }
}
