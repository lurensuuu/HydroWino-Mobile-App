package com.example.hydrowino;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PasswordandSecurity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passwordand_security);

        TextView changeEmailbt = findViewById(R.id.changeEmailBt);
        TextView changePassbt = findViewById(R.id.changePassBt);
        ImageView backSecurity = findViewById(R.id.backSecurity);

        backSecurity.setOnClickListener(v -> finish());

        changeEmailbt.setOnClickListener(v -> showEmailDialog());

        changePassbt.setOnClickListener(v -> {
            Intent intent = new Intent(this, changePass.class);
            startActivity(intent);
        });
    }

    private void showEmailDialog() {
        // Inflate custom layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_email, null);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Set background to handle rounded corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_bg);
        }

        // Display Masked Email
        TextView tvUserEmail = dialogView.findViewById(R.id.userEmail);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            tvUserEmail.setText(maskEmail(user.getEmail()));
        }

        // Set up the Change Email button inside the custom layout
        Button btnChangeEmail = dialogView.findViewById(R.id.btnChangeEmail);
        btnChangeEmail.setOnClickListener(v -> {
            Intent intent = new Intent(this, change_email.class);
            startActivity(intent);
            dialog.dismiss();
        });

        dialog.show();

        // Adjust dialog width
        int width = (int) (350 * getResources().getDisplayMetrics().density);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 1) return email; // Unlikely for valid email

        String namePart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (namePart.length() <= 2) {
            return namePart.charAt(0) + "***" + domainPart;
        } else {
            return namePart.charAt(0) + "***" + namePart.charAt(namePart.length() - 1) + domainPart;
        }
    }
}
