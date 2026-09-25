package com.example.hydrowino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeeklyRecord {
    private int weekNumber;
    private double avgTemperature;
    private double avgHumidity;
    private double avgPh;
    private double avgGrowthSize;
    private String dateRange;
    private List<GrowthRecord> dailyRecords;

    public WeeklyRecord(int weekNumber) {
        this.weekNumber = weekNumber;
        this.dailyRecords = new ArrayList<>();
    }

    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public double getAvgTemperature() { return avgTemperature; }
    public void setAvgTemperature(double avgTemperature) { this.avgTemperature = avgTemperature; }

    public double getAvgHumidity() { return avgHumidity; }
    public void setAvgHumidity(double avgHumidity) { this.avgHumidity = avgHumidity; }

    public double getAvgPh() { return avgPh; }
    public void setAvgPh(double avgPh) { this.avgPh = avgPh; }

    public double getAvgGrowthSize() { return avgGrowthSize; }
    public void setAvgGrowthSize(double avgGrowthSize) { this.avgGrowthSize = avgGrowthSize; }

    public String getDateRange() { return dateRange; }
    public void setDateRange(String dateRange) { this.dateRange = dateRange; }

    public List<GrowthRecord> getDailyRecords() { return dailyRecords; }
    public void addDailyRecord(GrowthRecord record) {
        this.dailyRecords.add(record);
    }

    public void calculateAverages() {
        if (dailyRecords.isEmpty()) return;
        
        // Ensure they are sorted by date
        Collections.sort(dailyRecords, (r1, r2) -> r1.getDate().compareTo(r2.getDate()));

        double totalTemp = 0, totalHumid = 0, totalPh = 0, totalGrowth = 0;
        int count = 0;
        for (GrowthRecord r : dailyRecords) {
            if (r.getDailyAverage() != null) {
                totalTemp += r.getDailyAverage().getAvgTemperature();
                totalHumid += r.getDailyAverage().getAvgHumidity();
                totalPh += r.getDailyAverage().getAvgPh();
                totalGrowth += r.getDailyAverage().getAvgGrowthSize();
                count++;
            }
        }
        if (count > 0) {
            this.avgTemperature = totalTemp / count;
            this.avgHumidity = totalHumid / count;
            this.avgPh = totalPh / count;
            this.avgGrowthSize = totalGrowth / count;
        }
        
        String start = dailyRecords.get(0).getDate();
        String end = dailyRecords.get(dailyRecords.size() - 1).getDate();
        this.dateRange = start.equals(end) ? start : start + " to " + end;
    }
}
