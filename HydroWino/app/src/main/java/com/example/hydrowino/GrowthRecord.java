package com.example.hydrowino;

import com.google.firebase.firestore.PropertyName;

public class GrowthRecord {
    private String day;
    private String date; // This will be the document ID
    private DailyAverage dailyAverage;

    public GrowthRecord() {}

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @PropertyName("DailyBestCondition")
    public DailyAverage getDailyAverage() { return dailyAverage; }

    @PropertyName("DailyBestCondition")
    public void setDailyAverage(DailyAverage dailyAverage) { this.dailyAverage = dailyAverage; }

    public static class DailyAverage {
        private String day;
        private double avgTemperature;
        private double avgHumidity;
        private double avgPh;
        private double avgGrowthSize;
        private String imageUrl;
        private java.util.List<PlantMeasurement> plants;

        public DailyAverage() {}

        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }

        public double getAvgTemperature() { return avgTemperature; }
        public void setAvgTemperature(double avgTemperature) { this.avgTemperature = avgTemperature; }

        public double getAvgHumidity() { return avgHumidity; }
        public void setAvgHumidity(double avgHumidity) { this.avgHumidity = avgHumidity; }

        public double getAvgPh() { return avgPh; }
        public void setAvgPh(double avgPh) { this.avgPh = avgPh; }

        public double getAvgGrowthSize() { return avgGrowthSize; }
        public void setAvgGrowthSize(double avgGrowthSize) { this.avgGrowthSize = avgGrowthSize; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public java.util.List<PlantMeasurement> getPlants() { return plants; }
        public void setPlants(java.util.List<PlantMeasurement> plants) { this.plants = plants; }
    }

    public static class PlantMeasurement {
        private long plantId;
        private double lengthCm;
        private double lengthInches;
        private double widthCm;
        private double widthInches;

        public PlantMeasurement() {}

        public long getPlantId() { return plantId; }
        public void setPlantId(long plantId) { this.plantId = plantId; }

        public double getLengthCm() { return lengthCm; }
        public void setLengthCm(double lengthCm) { this.lengthCm = lengthCm; }

        public double getLengthInches() { return lengthInches; }
        public void setLengthInches(double lengthInches) { this.lengthInches = lengthInches; }

        public double getWidthCm() { return widthCm; }
        public void setWidthCm(double widthCm) { this.widthCm = widthCm; }

        public double getWidthInches() { return widthInches; }
        public void setWidthInches(double widthInches) { this.widthInches = widthInches; }
    }
}
