package com.example.hydrowino;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class GreenhouseActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greenhouse);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ImageView backBtn = findViewById(R.id.backGreenhouse);
//        TextView connectBtn = findViewById(R.id.connectGreenhouseBt);
        TextView infoBtn = findViewById(R.id.greenhouseInfoBt);
        TextView leaveBtn = findViewById(R.id.leaveGreenhouseBt);

        backBtn.setOnClickListener(v -> finish());

//        connectBtn.setOnClickListener(v -> {
//            startActivity(new Intent(this, ConnectGreenhouseActivity.class));
//        });

        infoBtn.setOnClickListener(v -> {
            GreenhouseRepository.GreenhouseData data = GreenhouseRepository.getInstance().getCurrentGreenhouse();
            if (data != null) {
                String info = "Name: " + data.getGreenhouseName() + "\n" +
                             "ID: " + data.getGreenhouseID() + "\n" +
                             "Location: " + data.getLocation() + "\n" +
                             "Status: " + data.getStatus();
                
                new AlertDialog.Builder(this)
                        .setTitle("Greenhouse Information")
                        .setMessage(info)
                        .setPositiveButton("OK", null)
                        .show();
            } else {
                Toast.makeText(this, "No greenhouse connected", Toast.LENGTH_SHORT).show();
            }
        });

        leaveBtn.setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;

            new AlertDialog.Builder(this)
                    .setTitle("Leave Greenhouse")
                    .setMessage("Are you sure you want to disconnect from this greenhouse?")
                    .setPositiveButton("Yes", (dialog, which) -> leaveGreenhouse(user.getUid()))
                    .setNegativeButton("No", null)
                    .show();
        });
    }

    private void leaveGreenhouse(String uid) {
        GreenhouseRepository.GreenhouseData data = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (data == null || data.getDocumentId() == null) {
            Toast.makeText(this, "No active greenhouse connection found", Toast.LENGTH_SHORT).show();
            return;
        }

        String greenhouseDocId = data.getDocumentId();

        com.google.firebase.firestore.WriteBatch batch = db.batch();

        // 1. Remove from /GreenHouses/{greenhouseId}/members/{uid}
        batch.delete(db.collection("GreenHouses").document(greenhouseDocId)
                .collection("members").document(uid));

        // 2. Clear greenhouseID from /users/{uid}
        batch.update(db.collection("users").document(uid), "greenhouseID", null);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Disconnected from greenhouse", Toast.LENGTH_SHORT).show();
                    GreenhouseRepository.getInstance().startListening();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error disconnecting: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
