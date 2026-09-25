package com.example.hydrowino;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ConnectGreenhouseActivity extends BaseActivity {

    private EditText etGreenhouseID;
    private Button btnSendRequest;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_greenhouse);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etGreenhouseID = findViewById(R.id.etGreenhouseID);
        btnSendRequest = findViewById(R.id.btnSendRequest);
        progressBar = findViewById(R.id.progressBar);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnSendRequest.setOnClickListener(v -> connectGreenhouse());
    }

    private void connectGreenhouse() {
        String enteredPasskey = etGreenhouseID.getText().toString().trim().toUpperCase();

        if (TextUtils.isEmpty(enteredPasskey)) {
            Toast.makeText(this, "Please enter your greenhouse access code.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        btnSendRequest.setEnabled(false);
        btnSendRequest.setText("Connecting to greenhouse...");
        progressBar.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showError("Your session has expired. Please sign in again.");
            return;
        }

        String uid = currentUser.getUid();

        // 1. Check whether current user already has a greenhouse assigned
        db.collection("users").document(uid).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String existingGid = userDoc.getString("greenhouseID");
                        if (existingGid == null) existingGid = userDoc.getString("greenhouseId");
                        if (existingGid != null && !existingGid.isEmpty()) {
                            showError("Your account is already connected to a greenhouse.");
                            return;
                        }
                    }

                    // 2. Query /GreenHouses collection where joinPasskey == enteredPasskey
                    db.collection("GreenHouses")
                            .whereEqualTo("joinPasskey", enteredPasskey)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot.isEmpty()) {
                                    showError("Invalid greenhouse access code. Please check the code and try again.");
                                    return;
                                }

                                DocumentSnapshot ghDoc = querySnapshot.getDocuments().get(0);
                                String greenhouseId = ghDoc.getId(); // Automatically determined document ID

                                // Check if joinPasskeyEnabled == true
                                Boolean joinPasskeyEnabled = ghDoc.getBoolean("joinPasskeyEnabled");
                                if (joinPasskeyEnabled == null || !joinPasskeyEnabled) {
                                    showError("This greenhouse access code is currently disabled.");
                                    return;
                                }

                                // Check if status == "active"
                                String status = ghDoc.getString("status");
                                if (status == null || !status.equalsIgnoreCase("active")) {
                                    showError("This greenhouse is currently unavailable.");
                                    return;
                                }

                                // 3. Connect user to greenhouse
                                proceedWithConnection(greenhouseId, uid);
                            })
                            .addOnFailureListener(e -> showError("Unable to connect. Please check your internet connection and try again."));
                })
                .addOnFailureListener(e -> showError("Unable to connect. Please check your internet connection and try again."));
    }

    private void proceedWithConnection(String greenhouseId, String uid) {
        // Update /users/{uid}.greenhouseID without overwriting other fields
        Map<String, Object> userUpdate = new HashMap<>();
        userUpdate.put("greenhouseID", greenhouseId);

        db.collection("users").document(uid).update(userUpdate)
                .addOnSuccessListener(aVoid -> {
                    // Create /GreenHouses/{greenhouseId}/members/{uid}
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("userId", uid);
                    memberData.put("role", "cultivator");
                    memberData.put("joinedAt", com.google.firebase.Timestamp.now());
                    memberData.put("joinMethod", "passkey");

                    db.collection("GreenHouses").document(greenhouseId)
                            .collection("members").document(uid)
                            .set(memberData)
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(ConnectGreenhouseActivity.this, "Greenhouse connected successfully.", Toast.LENGTH_LONG).show();
                                
                                // Refresh GreenhouseRepo / Firestore synchronization
                                GreenhouseRepository.getInstance().startListening();
                                
                                // Open Home screen (finish activity since fragment handles dashboard)
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Rollback user assignment on member creation failure to avoid partial assignment
                                db.collection("users").document(uid).update("greenhouseID", null);
                                showError("Unable to connect. Please check your internet connection and try again.");
                            });
                })
                .addOnFailureListener(e -> showError("Unable to connect. Please check your internet connection and try again."));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        btnSendRequest.setEnabled(true);
        btnSendRequest.setText("Connect Greenhouse");
        progressBar.setVisibility(View.GONE);
    }
}