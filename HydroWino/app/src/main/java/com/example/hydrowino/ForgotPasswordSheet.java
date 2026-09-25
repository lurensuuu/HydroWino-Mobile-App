package com.example.hydrowino;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ForgotPasswordSheet extends BottomSheetDialogFragment {

    private EditText etEmail;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.activity_forgot_password_sheet, container, false);

        etEmail = v.findViewById(R.id.etForgotEmail);
        Button btnContinue = v.findViewById(R.id.btnForgotContinue);

        btnContinue.setOnClickListener(view -> {
            String email = etEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(getActivity(), "Please enter your email", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate 4-digit OTP
            String otp = String.format("%04d", new Random().nextInt(10000));

            // Send OTP to backend
            sendOtpToEmail(email, otp);
        });

        return v;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);

        dialog.setOnShowListener(dialogInterface -> {
            FrameLayout bottomSheet = dialog.findViewById(
                    com.google.android.material.R.id.design_bottom_sheet);

            if (bottomSheet != null) {
                // Apply your rounded corners drawable
                bottomSheet.setBackgroundResource(R.drawable.bottom_sheet_background);
                bottomSheet.setClipToOutline(true); // This ensures corners are clipped
            }
        });

        return dialog;
    }

    private void sendOtpToEmail(String email, String otp) {
        String url = "http://192.168.8.37/HydroWino_Backend/send_otp.php"; // Your backend URL

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    // Handle backend response
                    Toast.makeText(getActivity(), "OTP sent to " + email, Toast.LENGTH_SHORT).show();

                    // Go to ConfirmationCode activity
                    Intent intent = new Intent(getActivity(), ConfirmationCode.class);
                    intent.putExtra("email", email);
                    intent.putExtra("otp", otp); // Optional: use for local verification
                    startActivity(intent);
                    dismiss();
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getActivity(), "Failed to send OTP", Toast.LENGTH_SHORT).show();
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("email", email);
                params.put("otp", otp);
                return params;
            }
        };

        Volley.newRequestQueue(getActivity()).add(request);
    }
}