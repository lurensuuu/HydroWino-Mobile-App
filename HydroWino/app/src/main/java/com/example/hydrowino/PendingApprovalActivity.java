package com.example.hydrowino;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class PendingApprovalActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration approvalListener;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Configure Google Sign-In to allow sign out
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        TextView tvLogout = findViewById(R.id.tvLogout);
        tvLogout.setOnClickListener(v -> {
            // Sign out from Firebase
            mAuth.signOut();
            
            // Sign out from Google
            mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
                startActivity(new Intent(PendingApprovalActivity.this, loginActivity.class));
                finish();
            });
        });

        // Listen for approval status changes in the greenhouseApplication collection
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            
            approvalListener = db.collection("greenhouseApplication")
                    .whereEqualTo("userId", userId)
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null || value.isEmpty()) {
                            return;
                        }

                        // Check the most recent application
                        com.google.firebase.firestore.DocumentSnapshot doc = value.getDocuments().get(0);
                        String status = doc.getString("status");
                        Boolean isApproved = doc.getBoolean("isApproved");
                        String greenhouseId = doc.getString("greenhouseId");
                        
                        // If either isApproved is true or status is "approved"
                        if ((isApproved != null && isApproved) || "approved".equals(status)) {
                            // Sync status for consistency if isApproved was toggled manually
                            if (isApproved != null && isApproved && !"approved".equals(status)) {
                                doc.getReference().update("status", "approved");
                            }
                            proceedToApp(userId, greenhouseId);
                        }
                    });
        }
    }

    private void proceedToApp(String userId, String greenhouseId) {
        // Only proceed if greenhouseId is actually assigned by admin
        if (greenhouseId == null || greenhouseId.isEmpty()) {
            android.util.Log.w("PendingApproval", "Approved but greenhouseId is missing. Waiting for admin...");
            return; 
        }

        // Save greenhouse ID to SharedPreferences
        getSharedPreferences("HydroWinoPrefs", MODE_PRIVATE)
                .edit().putString("greenhouse_id", greenhouseId).apply();

        Toast.makeText(this, "Your account has been approved!", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (approvalListener != null) {
            approvalListener.remove();
        }
    }
}
