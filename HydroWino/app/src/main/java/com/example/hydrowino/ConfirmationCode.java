package com.example.hydrowino;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfirmationCode extends AppCompatActivity {

    private EditText otp1, otp2, otp3, otp4;
    private Button continueBtn;
    private TextView resendBtn;
    private String name, email, password;
    private FirebaseAuth mAuth;

    private static final String SCRIPT_URL = "https://script.google.com/macros/s/AKfycbzAOOHSNXnVcCm5iA9Fu612359R4ufAgLk0CTmhkIaWYAUVEx9Wlir2qJTseP-WpNj8/exec";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.confirmation_code);

        mAuth = FirebaseAuth.getInstance();
        name = getIntent().getStringExtra("name");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        otp1 = findViewById(R.id.otp1);
        otp2 = findViewById(R.id.otp2);
        otp3 = findViewById(R.id.otp3);
        otp4 = findViewById(R.id.otp4);
        continueBtn = findViewById(R.id.ContinueButton);
        resendBtn = findViewById(R.id.ResendButton);

        setupOTPInputs(otp1, otp2, null);
        setupOTPInputs(otp2, otp3, otp1);
        setupOTPInputs(otp3, otp4, otp2);
        setupOTPInputs(otp4, null, otp3);

        continueBtn.setOnClickListener(v -> verifyWithServer());
        resendBtn.setOnClickListener(v -> resendCode());
    }

    private void setupOTPInputs(EditText current, EditText next, EditText previous) {
        current.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) next.requestFocus();
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        current.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                if (current.getText().toString().isEmpty() && previous != null) {
                    previous.requestFocus();
                    return true;
                }
            }
            return false;
        });
    }

    private void verifyWithServer() {

        String otp = otp1.getText().toString() +
                otp2.getText().toString() +
                otp3.getText().toString() +
                otp4.getText().toString();

        if (otp.length() < 4) {
            Toast.makeText(this, "Enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {

                String url = SCRIPT_URL
                        + "?action=verify"
                        + "&email=" + URLEncoder.encode(email, "UTF-8")
                        + "&otp=" + otp;

                String response = performRequest(url);

                JSONObject json = new JSONObject(response);
                String status = json.optString("status");

                runOnUiThread(() -> {

                    switch (status) {
                        case "verified":
                            registerFirebase();
                            break;

                        case "expired":
                            showError("OTP expired");
                            break;

                        default:
                            showError("Invalid OTP");
                            break;
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> showError("Network error"));
            }
        });
    }

    private void registerFirebase() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finishAffinity();
                    } else {
                        showError(task.getException().getMessage());
                    }
                });
    }

    private void resendCode() {

        String newOtp = String.valueOf(1000 + new Random().nextInt(9000));

        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.execute(() -> {
            try {

                String url = SCRIPT_URL
                        + "?action=send"
                        + "&email=" + URLEncoder.encode(email, "UTF-8")
                        + "&code=" + newOtp;

                performRequest(url);

                runOnUiThread(() -> {
                    Toast.makeText(this, "New OTP sent", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Resend failed", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void showError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        continueBtn.setEnabled(true);
        continueBtn.setText("Continue");
    }

    private String performRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(true);
        int status = conn.getResponseCode();
        if (status >= 300 && status <= 308) {
            return performRequest(conn.getHeaderField("Location"));
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder res = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) res.append(line);
        in.close();
        return res.toString();
    }
}
