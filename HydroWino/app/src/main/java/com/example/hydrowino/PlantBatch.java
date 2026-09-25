package com.example.hydrowino;

import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;
import java.util.List;

public class PlantBatch implements Serializable {
    private String id;
    private String name;
    private String type;
    private String status;
    private String dateRange;
    private String duration;
    private double avgTemp;
    private double avgPh;
    private double avgHumidity;
    private double avgTds;
    private double avgWaterTemp;
    private String avgGrowth;
    private String waterUsed;
    private String imageUrl;
    private String greenhouseId;
    private List<Float> sparklineData;

    public PlantBatch() {}

    public String getGreenhouseId() { return greenhouseId; }
    public void setGreenhouseId(String greenhouseId) { this.greenhouseId = greenhouseId; }

    public String getAvgGrowth() { return avgGrowth; }
    public void setAvgGrowth(String avgGrowth) { this.avgGrowth = avgGrowth; }

    public String getWaterUsed() { return waterUsed; }
    public void setWaterUsed(String waterUsed) { this.waterUsed = waterUsed; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDateRange() { return dateRange; }
    public void setDateRange(String dateRange) { this.dateRange = dateRange; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public double getAvgTemp() { return avgTemp; }
    public void setAvgTemp(double avgTemp) { this.avgTemp = avgTemp; }

    @PropertyName("avgPH")
    public double getAvgPh() { return avgPh; }
    @PropertyName("avgPH")
    public void setAvgPh(double avgPh) { this.avgPh = avgPh; }

    public double getAvgHumidity() { return avgHumidity; }
    public void setAvgHumidity(double avgHumidity) { this.avgHumidity = avgHumidity; }

    @PropertyName("averageTDS")
    public double getAvgTds() { return avgTds; }
    @PropertyName("averageTDS")
    public void setAvgTds(double avgTds) { this.avgTds = avgTds; }

    public double getAvgWaterTemp() { return avgWaterTemp; }
    public void setAvgWaterTemp(double avgWaterTemp) { this.avgWaterTemp = avgWaterTemp; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @PropertyName("sparkLineData")
    public List<Float> getSparklineData() { return sparklineData; }
    @PropertyName("sparkLineData")
    public void setSparklineData(List<Float> sparklineData) { this.sparklineData = sparklineData; }
}
