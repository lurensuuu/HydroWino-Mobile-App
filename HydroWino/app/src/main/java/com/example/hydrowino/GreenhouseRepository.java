package com.example.hydrowino;

import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class GreenhouseRepository {

    private static final String TAG = "GreenhouseRepo";
    private static GreenhouseRepository instance;

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private ListenerRegistration greenhouseListener;
    private ListenerRegistration userListener;
    private GreenhouseData currentGreenhouse;
    private final java.util.List<OnGreenhouseDataChangedListener> listeners = new java.util.ArrayList<>();
    private String currentGreenhouseID;

    public interface OnGreenhouseDataChangedListener {
        void onDataChanged(GreenhouseData data);
    }

    private GreenhouseRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized GreenhouseRepository getInstance() {
        if (instance == null) {
            instance = new GreenhouseRepository();
        }
        return instance;
    }

    public void setListener(OnGreenhouseDataChangedListener listener) {
        if (listener != null) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
            listener.onDataChanged(currentGreenhouse);
        }
    }

    public void removeListener(OnGreenhouseDataChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void startListening() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "!! CRITICAL !! Cannot start listening: User not authenticated");
            return;
        }

        Log.i(TAG, ">> STARTING USER SYNC for UID: " + user.getUid());
        stopListening();

        // Listen to the user document for greenhouseID changes
        userListener = db.collection("users").document(user.getUid())
                .addSnapshotListener((userDoc, error) -> {
                    if (error != null) {
                        Log.e(TAG, "User document listen failed: ", error);
                        return;
                    }

                    if (userDoc != null && userDoc.exists()) {
                        String assignedId = userDoc.getString("greenhouseID");
                        if (assignedId == null) assignedId = userDoc.getString("greenhouseId");

                        if (assignedId != null && !assignedId.isEmpty()) {
                            if (!assignedId.equals(currentGreenhouseID)) {
                                currentGreenhouseID = assignedId;
                                listenToSpecificGreenhouse(assignedId, user.getUid());
                            }
                        } else {
                            Log.d(TAG, "No greenhouseID in user profile.");
                            currentGreenhouseID = null;
                            currentGreenhouse = null;
                            notifyListeners();
                        }
                    }
                });
    }

    public interface OnGreenhouseStatusListener extends OnGreenhouseDataChangedListener {
        void onPendingApplication(String greenhouseId);
    }

    private void listenToSpecificGreenhouse(String greenhouseId, String uid) {
        if (greenhouseListener != null) greenhouseListener.remove();

        greenhouseListener = db.collection("GreenHouses").document(greenhouseId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Greenhouse listen failed: ", error);
                        notifyListeners();
                        return;
                    }

                    if (doc != null && doc.exists()) {
                        processDocument(doc);
                        // Auto-claim if owner is missing
                        if (doc.getString("ownerId") == null) {
                            doc.getReference().update("ownerId", uid);
                        }
                    } else {
                        Log.e(TAG, "Greenhouse document does not exist: " + greenhouseId);
                        currentGreenhouse = null;
                        notifyListeners();
                    }
                });
    }

    private void processDocument(DocumentSnapshot doc) {
        try {
            // Manual parsing to avoid cast exceptions with plantingStartTime (Timestamp vs Long)
            GreenhouseData data = new GreenhouseData();
            data.setDocumentId(doc.getId());
            data.setGreenhouseID(doc.getString("greenhouseID"));
            data.setGreenhouseName(doc.getString("greenhouseName"));
            data.setDeviceId(doc.getString("deviceId"));
            data.setOwnerId(doc.getString("ownerId"));
            data.setLocation(doc.getString("location"));
            data.setCurrentBatch(doc.getString("currentBatch"));
            data.setStatus(doc.getString("status"));
            
            // Safe boolean check
            Boolean harvested = doc.getBoolean("isHarvested");
            data.setHarvested(harvested != null && harvested);
            
            // Safe Timestamp check (Handle both Timestamp and Number to prevent crashes)
            Object startTime = doc.get("plantingStartTime");
            if (startTime instanceof com.google.firebase.Timestamp) {
                data.setPlantingStartTime((com.google.firebase.Timestamp) startTime);
            } else if (startTime instanceof Number) {
                data.setPlantingStartTime(new com.google.firebase.Timestamp(new java.util.Date(((Number) startTime).longValue())));
            }

            currentGreenhouse = data;
            
            Log.i(TAG, "Sync active: ID=" + data.getGreenhouseID() +
                    " Batch=" + data.getCurrentBatch() +
                    " Harvested=" + data.isHarvested());

            notifyListeners();
        } catch (Exception e) {
            Log.e(TAG, "Critical error parsing greenhouse document", e);
        }
    }

    private void notifyListeners() {
        for (OnGreenhouseDataChangedListener l : new java.util.ArrayList<>(listeners)) {
            if (l != null) {
                l.onDataChanged(currentGreenhouse);
            }
        }
    }

    public void stopListening() {
        if (greenhouseListener != null) {
            greenhouseListener.remove();
            greenhouseListener = null;
        }
        if (userListener != null) {
            userListener.remove();
            userListener = null;
        }
        currentGreenhouseID = null;
    }

    public void logout() {
        stopListening();
        currentGreenhouse = null;
        listeners.clear();
    }

    public GreenhouseData getCurrentGreenhouse() {
        return currentGreenhouse;
    }

    public void updateCurrentBatch(String newBatch) {
        if (currentGreenhouse == null || currentGreenhouse.getDocumentId() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentBatch", newBatch);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("GreenHouses").document(currentGreenhouse.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Batch updated successfully: " + newBatch))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating batch", e));
    }

    public void updateHarvestStatus(boolean harvested) {
        if (currentGreenhouse == null || currentGreenhouse.getDocumentId() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("isHarvested", harvested);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        if (!harvested) {
             updates.put("plantingStartTime", com.google.firebase.Timestamp.now());
        }

        db.collection("GreenHouses").document(currentGreenhouse.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Harvest status updated successfully: " + harvested))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating harvest status", e));
    }

    public void updatePlantingStartTime(long timestampMillis) {
        if (currentGreenhouse == null || currentGreenhouse.getDocumentId() == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("plantingStartTime", new com.google.firebase.Timestamp(new java.util.Date(timestampMillis)));
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.collection("GreenHouses").document(currentGreenhouse.getDocumentId())
                .update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Planting start time updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating planting start time", e));
    }

    public static class GreenhouseData {
        private String greenhouseID;
        private String greenhouseName;
        private String deviceId;
        private String ownerId;
        private String location;
        private String currentBatch;
        private boolean isHarvested;
        private String status;
        private com.google.firebase.Timestamp plantingStartTime;
        private com.google.firebase.Timestamp createdAt;
        private com.google.firebase.Timestamp updatedAt;
        private String documentId;

        public GreenhouseData() {}

        @PropertyName("greenhouseID")
        public String getGreenhouseID() { return greenhouseID; }
        @PropertyName("greenhouseID")
        public void setGreenhouseID(String greenhouseID) { this.greenhouseID = greenhouseID; }

        public String getGreenhouseName() { return greenhouseName; }
        public void setGreenhouseName(String greenhouseName) { this.greenhouseName = greenhouseName; }
        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
        public String getOwnerId() { return ownerId; }
        public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getCurrentBatch() { return currentBatch; }
        public void setCurrentBatch(String currentBatch) { this.currentBatch = currentBatch; }

        @PropertyName("isHarvested")
        public boolean isHarvested() { return isHarvested; }
        @PropertyName("isHarvested")
        public void setHarvested(boolean harvested) { isHarvested = harvested; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public com.google.firebase.Timestamp getPlantingStartTime() { return plantingStartTime; }
        public void setPlantingStartTime(com.google.firebase.Timestamp plantingStartTime) { this.plantingStartTime = plantingStartTime; }

        public com.google.firebase.Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(com.google.firebase.Timestamp createdAt) { this.createdAt = createdAt; }

        public com.google.firebase.Timestamp getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(com.google.firebase.Timestamp updatedAt) { this.updatedAt = updatedAt; }

        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
    }
}
