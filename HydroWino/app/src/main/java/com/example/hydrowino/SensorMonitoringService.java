package com.example.hydrowino;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.firebase.Timestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class SensorMonitoringService extends Service {

    private FirebaseFirestore firestore;
    private DatabaseReference rtdbReference;
    private ValueEventListener sensorListener;
    private Map<Integer, Long> alertTimestamps = new HashMap<>();
    private SharedPreferences sharedPreferences;

    private static final long REMINDER_INTERVAL = 3 * 60 * 1000; // 3 minutes reminder interval

    private SensorData lastKnownData = new SensorData(); // Cache latest values

    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("HydroWinoPrefs", MODE_PRIVATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, "hydrowino_alerts")
                .setContentTitle("HydroWino Monitoring")
                .setSmallIcon(R.drawable.ic_warning)
                .build();

        startForeground(100, notification);
        startListening();

        return START_STICKY;
    }

    private void startListening() {
        GreenhouseRepository.getInstance().setListener(data -> {
            if (data != null && data.getGreenhouseID() != null) {
                String greenhouseId = data.getGreenhouseID();
                if (rtdbReference == null || !rtdbReference.toString().contains(greenhouseId)) {
                    if (rtdbReference != null && sensorListener != null) {
                        rtdbReference.removeEventListener(sensorListener);
                    }
                    rtdbReference = FirebaseDatabase.getInstance().getReference("IoT").child(greenhouseId).child("sensors");
                    
                    sensorListener = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            SensorData sensorData = snapshot.getValue(SensorData.class);
                            if (sensorData != null) {
                                lastKnownData = sensorData;
                                checkAlerts(sensorData);
                                updateMinMaxTracking(sensorData);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    };
                    rtdbReference.addValueEventListener(sensorListener);
                }
            } else {
                // If no greenhouse ID, stop listening to RTDB
                if (rtdbReference != null && sensorListener != null) {
                    rtdbReference.removeEventListener(sensorListener);
                    rtdbReference = null;
                }
            }
        });
    }

    private void updateMinMaxTracking(SensorData data) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        float minT = sharedPreferences.getFloat("t_min", Float.MAX_VALUE);
        float maxT = sharedPreferences.getFloat("t_max", Float.MIN_VALUE);
        if (data.airTemp < minT) editor.putFloat("t_min", data.airTemp);
        if (data.airTemp > maxT) editor.putFloat("t_max", data.airTemp);
        editor.apply();
    }

    private void resetMinMaxTracking() {
        sharedPreferences.edit()
                .putFloat("t_min", Float.MAX_VALUE).putFloat("t_max", Float.MIN_VALUE)
                .apply();
    }

    private void saveHourlyRecord(SensorData data, long timestamp) {
        // Disabled: The website Admin/backend is the only authorized system responsible for generating and saving hourly records.
    }

    private void calculateDailyAverage(String dateStr) {
        GreenhouseRepository.GreenhouseData ghData = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (ghData == null) return;
        
        String currentBatch = ghData.getCurrentBatch();
        String greenhouseId = ghData.getGreenhouseID();
        firestore.collection("PlantGrowthData")
                .document(greenhouseId)
                .collection("Batches")
                .document(currentBatch)
                .collection("Records")
                .document(dateStr)
                .collection("OneHourlyRecords")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) return;

                    double totalTemp = 0;
                    double totalHumid = 0;
                    double totalPh = 0;
                    double totalTds = 0;
                    double totalWaterTemp = 0;
                    int count = queryDocumentSnapshots.size();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        totalTemp += doc.getDouble("temperature") != null ? doc.getDouble("temperature") : 0;
                        totalHumid += doc.getDouble("humidity") != null ? doc.getDouble("humidity") : 0;
                        totalPh += doc.getDouble("phLevel") != null ? doc.getDouble("phLevel") : 0;
                        totalTds += doc.getDouble("tds") != null ? doc.getDouble("tds") : 0;
                        totalWaterTemp += doc.getDouble("waterTemperature") != null ? doc.getDouble("waterTemperature") : 0;
                    }

                    final double avgTemp = totalTemp / count;
                    final double avgHumid = totalHumid / count;
                    final double avgPh = totalPh / count;
                    final double avgTds = totalTds / count;
                    final double avgWaterTemp = totalWaterTemp / count;

                    // Now fetch Growth scans for the same day to calculate avgGrowthSize
                    fetchAndSaveGrowthAverage(dateStr, avgTemp, avgHumid, avgPh, avgTds, avgWaterTemp);
                });
    }

    private void fetchAndSaveGrowthAverage(String dateStr, double avgTemp, double avgHumid, double avgPh, double avgTds, double avgWaterTemp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date recordDate = sdf.parse(dateStr);
            if (recordDate == null) return;

            Calendar cal = Calendar.getInstance();
            cal.setTime(recordDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Timestamp startTimestamp = new Timestamp(cal.getTime());

            cal.add(Calendar.DAY_OF_MONTH, 1);
            Timestamp endTimestamp = new Timestamp(cal.getTime());

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        firestore.collection("scans")
                .whereEqualTo("userId", user.getUid())
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThan("timestamp", endTimestamp)
                .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        double totalGrowth = 0;
                        int scanCount = queryDocumentSnapshots.size();
                        String bestImageUrl = null;
                        double maxConfidence = -1;

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            double growth = doc.getDouble("confidence") != null ? doc.getDouble("confidence") : 0;
                            totalGrowth += growth;

                            // Keep track of the image with the highest confidence/growth for the day
                            if (growth > maxConfidence) {
                                maxConfidence = growth;
                                bestImageUrl = doc.getString("imageUrl");
                            }
                        }

                        double avgGrowth = scanCount > 0 ? (totalGrowth / scanCount) : 0;
                        final String finalImageUrl = bestImageUrl;

                        GreenhouseRepository.GreenhouseData ghData = GreenhouseRepository.getInstance().getCurrentGreenhouse();
                        long plantingStart = (ghData != null && ghData.getPlantingStartTime() != null) ?
                                ghData.getPlantingStartTime().toDate().getTime() : 0;

                        if (plantingStart == 0) {
                            // Try to recover from existing data if possible
                            String currentBatch = ghData != null ? ghData.getCurrentBatch() : null;
                            String greenhouseId = ghData != null ? ghData.getGreenhouseID() : null;
                            
                            if (greenhouseId == null || currentBatch == null) {
                                finalizeAverageSave(dateStr, avgTemp, avgHumid, avgPh, avgTds, avgWaterTemp, avgGrowth, finalImageUrl, recordDate.getTime(), recordDate);
                                return;
                            }
                            firestore.collection("PlantGrowthData")
                                    .document(greenhouseId)
                                    .collection("Batches")
                                    .document(currentBatch)
                                    .collection("Records")
                                    .orderBy("DailyBestCondition.timestamp", Query.Direction.ASCENDING)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener(snapshot -> {
                                        long recoveredStart;
                                        if (!snapshot.isEmpty()) {
                                            Timestamp ts = snapshot.getDocuments().get(0).getTimestamp("DailyBestCondition.timestamp");
                                            recoveredStart = (ts != null) ? ts.toDate().getTime() : recordDate.getTime();
                                        } else {
                                            recoveredStart = recordDate.getTime();
                                        }
                                        sharedPreferences.edit().putLong("planting_start_time", recoveredStart).apply();
                                        finalizeAverageSave(dateStr, avgTemp, avgHumid, avgPh, avgTds, avgWaterTemp, avgGrowth, finalImageUrl, recoveredStart, recordDate);
                                    })
                                    .addOnFailureListener(e -> {
                                        finalizeAverageSave(dateStr, avgTemp, avgHumid, avgPh, avgTds, avgWaterTemp, avgGrowth, finalImageUrl, recordDate.getTime(), recordDate);
                                    });
                        } else {
                            finalizeAverageSave(dateStr, avgTemp, avgHumid, avgPh, avgTds, avgWaterTemp, avgGrowth, finalImageUrl, plantingStart, recordDate);
                        }
                    });

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void finalizeAverageSave(String dateStr, double avgTemp, double avgHumid, double avgPh, double avgTds, double avgWaterTemp, double avgGrowth, String imageUrl, long plantingStart, Date recordDate) {
        GreenhouseRepository.GreenhouseData ghData = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (ghData == null) return;

        // Calculate day count based on the record's date, not "now"
        long diff = recordDate.getTime() - plantingStart;
        long dayCount = (diff / (1000 * 60 * 60 * 24)) + 1;

        // Ensure dayCount is at least 1, and handle potential future record dates gracefully
        if (dayCount < 1) dayCount = 1;

        Map<String, Object> averages = new HashMap<>();
        averages.put("day", "Day " + dayCount);
        averages.put("avgTemperature", (float) avgTemp);
        averages.put("avgHumidity", (float) avgHumid);
        averages.put("avgPh", (float) avgPh);
        averages.put("avgTds", (float) avgTds);
        averages.put("avgWaterTemp", (float) avgWaterTemp);
        averages.put("avgGrowthSize", (float) avgGrowth);
        averages.put("imageUrl", imageUrl); // Added image field
        averages.put("timestamp", new Timestamp(recordDate)); // Use record date for timestamp!

        Map<String, Object> update = new HashMap<>();
        update.put("DailyBestCondition", averages);

        String currentBatch = ghData.getCurrentBatch();
        String greenhouseId = ghData.getGreenhouseID();
        firestore.collection("PlantGrowthData")
                .document(greenhouseId)
                .collection("Batches")
                .document(currentBatch)
                .collection("Records")
                .document(dateStr)
                .set(update, SetOptions.merge());
    }

    private void checkAlerts(SensorData data) {
        // Water Level Alerts (Normal: 15-35L, Low: 1-14L, High: 36-40L)
        if (data.waterLevel > 35) {
            handleAlert(10, getString(R.string.alert_water_title), getString(R.string.alert_water_overflow_msg), "water", data.waterLevel);
            handleRecovery(1, getString(R.string.alert_water_title), "water"); // Recover 'Low' if it becomes 'High'
        } else if (data.waterLevel < 15) {
            handleAlert(1, getString(R.string.alert_water_title), getString(R.string.alert_water_low_msg), "water", data.waterLevel);
            handleRecovery(10, getString(R.string.alert_water_title), "water"); // Recover 'High' if it becomes 'Low'
        } else {
            handleRecovery(10, getString(R.string.alert_water_title), "water");
            handleRecovery(1, getString(R.string.alert_water_title), "water");
        }

        // Air Temperature Alerts
        if (data.airTemp > 28) handleAlert(2, getString(R.string.alert_temp_title), getString(R.string.alert_temp_high_msg), "temperature", data.airTemp);
        else handleRecovery(2, getString(R.string.alert_temp_title), "temperature");

        if (data.airTemp < 12) handleAlert(3, getString(R.string.alert_temp_title), getString(R.string.alert_temp_low_msg), "temperature", data.airTemp);
        else handleRecovery(3, getString(R.string.alert_temp_title), "temperature");

        // Humidity Alerts
        if (data.humidity >= 80) handleAlert(4, getString(R.string.alert_humid_title), getString(R.string.alert_humid_high_msg), "humidity", data.humidity);
        else if (data.humidity < 50) handleAlert(5, getString(R.string.alert_humid_title), getString(R.string.alert_humid_low_msg), "humidity", data.humidity);
        else {
            handleRecovery(4, getString(R.string.alert_humid_title), "humidity");
            handleRecovery(5, getString(R.string.alert_humid_title), "humidity");
        }

        // pH Alerts
        if (data.ph < 5.5) handleAlert(6, getString(R.string.alert_ph_title), getString(R.string.alert_ph_acidic_msg), "ph", (float)data.ph);
        else if (data.ph > 6.5) handleAlert(11, getString(R.string.alert_ph_title), getString(R.string.alert_ph_alkaline_msg), "ph", (float)data.ph);
        else {
            handleRecovery(6, getString(R.string.alert_ph_title), "ph");
            handleRecovery(11, getString(R.string.alert_ph_title), "ph");
        }

        // TDS Alerts
        if (data.tds > 1200) handleAlert(7, getString(R.string.alert_tds_title), getString(R.string.alert_tds_high_msg), "tds", data.tds);
        else if (data.tds < 500) handleAlert(8, getString(R.string.alert_tds_title), getString(R.string.alert_tds_low_msg), "tds", data.tds);
        else {
            handleRecovery(7, getString(R.string.alert_tds_title), "tds");
            handleRecovery(8, getString(R.string.alert_tds_title), "tds");
        }

        // Water Temperature Alerts
        if (data.waterTemp > 25) handleAlert(9, getString(R.string.alert_watertemp_title), getString(R.string.alert_watertemp_high_msg), "water_temp", data.waterTemp);
        else if (data.waterTemp < 18) handleAlert(12, getString(R.string.alert_watertemp_title), getString(R.string.alert_watertemp_low_msg), "water_temp", data.waterTemp);
        else {
            handleRecovery(9, getString(R.string.alert_watertemp_title), "water_temp");
            handleRecovery(12, getString(R.string.alert_watertemp_title), "water_temp");
        }
    }

    private void handleAlert(int id, String title, String message, String type, float currentValue) {
        long now = System.currentTimeMillis();
        boolean shouldNotify = false;

        if (!alertTimestamps.containsKey(id)) {
            // Initial Alert
            shouldNotify = true;
        } else {
            // Check Time Interval (5 mins reminder)
            Long lastNotifyTime = alertTimestamps.get(id);
            if (lastNotifyTime != null && (now - lastNotifyTime >= REMINDER_INTERVAL)) {
                shouldNotify = true;
            }
        }

        if (shouldNotify) {
            boolean isReminder = alertTimestamps.containsKey(id);
            alertTimestamps.put(id, now);
            
            String finalTitle = isReminder ? getString(R.string.alert_reminder_prefix) + title : title;

            NotificationHelper.showNotification(this, finalTitle, message + " (" + currentValue + ")", id);
            saveLogToFirestore(finalTitle, message + " (" + currentValue + ")", type);
        }
    }

    private void handleRecovery(int id, String title, String type) {
        if (alertTimestamps.containsKey(id)) {
            alertTimestamps.remove(id);
            String recoveryTitle = getString(R.string.alert_recovered_title);
            String recoveryMsg = getString(R.string.alert_recovered_msg);
            NotificationHelper.showNotification(this, title, recoveryMsg, id);
            saveLogToFirestore(title, recoveryMsg, type);
        }
    }

    private void saveLogToFirestore(String title, String message, String type) {
        GreenhouseRepository.GreenhouseData ghData = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (ghData == null) return;
        
        String greenhouseId = ghData.getGreenhouseID();
        Map<String, Object> log = new HashMap<>();
        log.put("title", title);
        log.put("message", message);
        log.put("timestamp", System.currentTimeMillis());
        log.put("type", type);
        log.put("read", false);
        firestore.collection("notification_logs")
                .document(greenhouseId)
                .collection("Logs")
                .add(log);
    }

    @Override
    public void onDestroy() {
        if (rtdbReference != null && sensorListener != null) rtdbReference.removeEventListener(sensorListener);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
