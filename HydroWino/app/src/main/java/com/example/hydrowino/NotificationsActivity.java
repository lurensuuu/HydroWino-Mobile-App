package com.example.hydrowino;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationsActivity extends BaseActivity {

    private CardView cardWater, cardTemp, cardHumid, cardPh, cardTds, cardWaterTemp;
    private TextView tvWater, tvTemp, tvHumid, tvPh, tvTds, tvWaterTemp;
    private View layoutNoAlerts, tvDeleteAllRead, tvReadAll;
    private DatabaseReference databaseReference;
    private ValueEventListener sensorListener;

    private RecyclerView rvLogs;
    private NotificationLogAdapter logAdapter;
    private List<NotificationLog> logList;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private String greenhouseId;
    private com.google.firebase.firestore.ListenerRegistration logsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification);

        cardWater = findViewById(R.id.cardWaterAlert);
        cardTemp = findViewById(R.id.cardTempAlert);
        cardHumid = findViewById(R.id.cardHumidAlert);
        cardPh = findViewById(R.id.cardPhAlert);
        cardTds = findViewById(R.id.cardTdsAlert);
        cardWaterTemp = findViewById(R.id.cardWaterTempAlert);

        tvWater = findViewById(R.id.tvWaterAlert);
        tvTemp = findViewById(R.id.tvTempAlert);
        tvHumid = findViewById(R.id.tvHumidAlert);
        tvPh = findViewById(R.id.tvPhAlert);
        tvTds = findViewById(R.id.tvTdsAlert);
        tvWaterTemp = findViewById(R.id.tvWaterTempAlert);
        layoutNoAlerts = findViewById(R.id.layoutNoAlerts);
        tvDeleteAllRead = findViewById(R.id.tvDeleteAllRead);
        tvReadAll = findViewById(R.id.tvReadAll);

        ImageView backBtn = findViewById(R.id.backLang);
        if (backBtn != null) {
            backBtn.setOnClickListener(v -> finish());
        }

        rvLogs = findViewById(R.id.rvNotificationLogs);
        logList = new ArrayList<>();
        
        logAdapter = new NotificationLogAdapter(logList, new NotificationLogAdapter.OnLogClickListener() {
            @Override
            public void onLogClick(NotificationLog log) {
                showNotificationModal(log);
                markAsRead(log);
            }

            @Override
            public void onDeleteClick(NotificationLog log) {
                confirmDeleteSingle(log);
            }
        });
        
        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        rvLogs.setAdapter(logAdapter);

        sharedPreferences = getSharedPreferences("HydroWinoPrefs", MODE_PRIVATE);
        firestore = FirebaseFirestore.getInstance(); // Initialize firestore before repository listener triggers!
        
        GreenhouseRepository.getInstance().setListener(data -> {
            if (data != null && data.getGreenhouseID() != null) {
                String newId = data.getGreenhouseID();
                if (greenhouseId == null || !greenhouseId.equals(newId)) {
                    greenhouseId = newId;
                    
                    // Update DB references
                    if (databaseReference != null && sensorListener != null) {
                        databaseReference.removeEventListener(sensorListener);
                    }
                    databaseReference = FirebaseDatabase.getInstance().getReference("IoT").child(greenhouseId).child("sensors");
                    
                    // Re-start listeners
                    startListening();
                    fetchLogs();
                }
            }
        });

        tvDeleteAllRead.setOnClickListener(v -> confirmDeleteAllRead());
        tvReadAll.setOnClickListener(v -> markAllAsRead());
    }

    private void showNotificationModal(NotificationLog log) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        String dateString = sdf.format(new Date(log.getTimestamp()));

        new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
                .setTitle(log.getTitle())
                .setMessage(log.getMessage() + "\n\nTime: " + dateString)
                .setPositiveButton("Close", null)
                .show();
    }

    private void markAsRead(NotificationLog log) {
        if (!log.isRead() && log.getId() != null) {
            firestore.collection("notification_logs")
                    .document(greenhouseId)
                    .collection("Logs")
                    .document(log.getId())
                    .update("read", true);
        }
    }

    private void markAllAsRead() {
        firestore.collection("notification_logs")
                .document(greenhouseId)
                .collection("Logs")
                .whereEqualTo("read", false)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No unread notifications", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    WriteBatch batch = firestore.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.update(doc.getReference(), "read", true);
                    }

                    batch.commit().addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "All marked as read", Toast.LENGTH_SHORT).show()
                    );
                });
    }

    private void confirmDeleteSingle(NotificationLog log) {
        new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> deleteLog(log))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteLog(NotificationLog log) {
        if (log.getId() != null) {
            firestore.collection("notification_logs")
                    .document(greenhouseId)
                    .collection("Logs")
                    .document(log.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Log deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error deleting log", Toast.LENGTH_SHORT).show());
        }
    }

    private void confirmDeleteAllRead() {
        firestore.collection("notification_logs")
                .document(greenhouseId)
                .collection("Logs")
                .whereEqualTo("read", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No read notifications to clear", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new MaterialAlertDialogBuilder(this, R.style.CustomAlertDialog)
                            .setTitle("Clear Read Notifications")
                            .setMessage("Are you sure you want to delete all read notifications?")
                            .setPositiveButton("Clear All", (dialog, which) -> deleteAllReadLogs())
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }

    private void deleteAllReadLogs() {
        firestore.collection("notification_logs")
                .document(greenhouseId)
                .collection("Logs")
                .whereEqualTo("read", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    WriteBatch batch = firestore.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        batch.delete(doc.getReference());
                    }

                    batch.commit().addOnSuccessListener(aVoid -> 
                        Toast.makeText(this, "Read notifications cleared", Toast.LENGTH_SHORT).show()
                    );
                });
    }

    private void startListening() {
        sensorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SensorData data = snapshot.getValue(SensorData.class);
                if (data != null) {
                    updateUI(data);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        databaseReference.addValueEventListener(sensorListener);
    }

    private void fetchLogs() {
        if (logsListener != null) logsListener.remove();
        
        logsListener = firestore.collection("notification_logs")
                .document(greenhouseId)
                .collection("Logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        logList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            NotificationLog log = doc.toObject(NotificationLog.class);
                            if (log != null) {
                                log.setId(doc.getId());
                                logList.add(log);
                            }
                        }
                        logAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void updateUI(SensorData data) {
        boolean hasAlert = false;

        // Water Alert
        if (data.waterLevel > 35) { // High Alert (> 35L)
            cardWater.setVisibility(View.VISIBLE);
            tvWater.setText(getString(R.string.alert_water_overflow_msg) + " (" + (int)data.waterLevel + "L)");
            hasAlert = true;
        } else if (data.waterLevel < 15) { // Low Alert (< 15L)
            cardWater.setVisibility(View.VISIBLE);
            tvWater.setText(getString(R.string.alert_water_low_msg) + " (" + (int)data.waterLevel + "L)");
            hasAlert = true;
        } else {
            cardWater.setVisibility(View.GONE);
        }

        // Room Temp Alert
        if (data.airTemp > 28) {
            cardTemp.setVisibility(View.VISIBLE);
            tvTemp.setText(getString(R.string.alert_temp_high_msg) + " (" + (int)data.airTemp + "°C)");
            hasAlert = true;
        } else if (data.airTemp < 12) {
            cardTemp.setVisibility(View.VISIBLE);
            tvTemp.setText(getString(R.string.alert_temp_low_msg) + " (" + (int)data.airTemp + "°C)");
            hasAlert = true;
        } else {
            cardTemp.setVisibility(View.GONE);
        }

        // Humidity Alert
        if (data.humidity >= 80) {
            cardHumid.setVisibility(View.VISIBLE);
            tvHumid.setText(getString(R.string.alert_humid_high_msg) + " (" + (int)data.humidity + "%)");
            hasAlert = true;
        } else if (data.humidity < 50) {
            cardHumid.setVisibility(View.VISIBLE);
            tvHumid.setText(getString(R.string.alert_humid_low_msg) + " (" + (int)data.humidity + "%)");
            hasAlert = true;
        } else {
            cardHumid.setVisibility(View.GONE);
        }

        // pH Alert
        if (data.ph < 5.5) {
            cardPh.setVisibility(View.VISIBLE);
            tvPh.setText(getString(R.string.alert_ph_acidic_msg) + " (pH " + String.format("%.1f", data.ph) + ")");
            hasAlert = true;
        } else if (data.ph > 6.5) {
            cardPh.setVisibility(View.VISIBLE);
            tvPh.setText(getString(R.string.alert_ph_alkaline_msg) + " (pH " + String.format("%.1f", data.ph) + ")");
            hasAlert = true;
        } else {
            cardPh.setVisibility(View.GONE);
        }

        // TDS Alert
        if (data.tds > 1200) {
            cardTds.setVisibility(View.VISIBLE);
            tvTds.setText(getString(R.string.alert_tds_high_msg) + " (" + (int)data.tds + " ppm)");
            hasAlert = true;
        } else if (data.tds < 500) {
            cardTds.setVisibility(View.VISIBLE);
            tvTds.setText(getString(R.string.alert_tds_low_msg) + " (" + (int)data.tds + " ppm)");
            hasAlert = true;
        } else {
            cardTds.setVisibility(View.GONE);
        }

        // Water Temp Alert
        if (data.waterTemp > 25) {
            cardWaterTemp.setVisibility(View.VISIBLE);
            tvWaterTemp.setText(getString(R.string.alert_watertemp_high_msg) + " (" + (int)data.waterTemp + "°C)");
            hasAlert = true;
        } else if (data.waterTemp < 18) {
            cardWaterTemp.setVisibility(View.VISIBLE);
            tvWaterTemp.setText(getString(R.string.alert_watertemp_low_msg) + " (" + (int)data.waterTemp + "°C)");
            hasAlert = true;
        } else {
            cardWaterTemp.setVisibility(View.GONE);
        }

        layoutNoAlerts.setVisibility(hasAlert ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseReference != null && sensorListener != null) {
            databaseReference.removeEventListener(sensorListener);
        }
        if (logsListener != null) {
            logsListener.remove();
        }
    }
}