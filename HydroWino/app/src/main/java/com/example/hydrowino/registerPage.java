package com.example.hydrowino;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

public class registerPage extends Fragment {

    private EditText etName, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout passwordInputLayout, confirmPasswordInputLayout;
    private Button signupBtn, googleBtn;
    private TextView haveAccountText;
    private CheckBox checkBox;

    private final String REGISTER_URL = "http://192.168.8.37/HydroWino_Backend/register.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register_page, container, false);

        etName = view.findViewById(R.id.etName);
        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword);
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = view.findViewById(R.id.confirmPasswordInputLayout);
        signupBtn = view.findViewById(R.id.SignupButton);
        googleBtn = view.findViewById(R.id.GoogleButton);
        haveAccountText = view.findViewById(R.id.HaveanAccount);
        checkBox = view.findViewById(R.id.checkBox);

        signupBtn.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Clear errors
            etName.setError(null);
            etEmail.setError(null);
            passwordInputLayout.setError(null);
            confirmPasswordInputLayout.setError(null);

            if(TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (name.matches(".*[^a-zA-Z\\sñÑ.-].*")) {
                etName.setError("Name can only contain letters, spaces, dots, and hyphens.");
                return;
            }

            // Strict Email Validation
            String emailPattern = "^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
            if (!email.matches(emailPattern)) {
                etEmail.setError("Please enter a valid email address (e.g., user@example.com).");
                return;
            }

            // Password Strength Requirements
            String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!_\\-*/?<>.,:;()\\\\~`|\\[\\]{}])[A-Za-z\\d@#$%^&+=!_\\-*/?<>.,:;()\\\\~`|\\[\\]{}]{8,64}$";
            if (!password.matches(passwordPattern)) {
                passwordInputLayout.setError("Password must be 8-64 characters with uppercase, lowercase, number, and special character.");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordInputLayout.setError("Passwords do not match.");
                return;
            }

            if(!checkBox.isChecked()) {
                Toast.makeText(getActivity(), "You must agree to the Terms & Privacy Policy", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(name, email, password);
        });

        haveAccountText.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), loginActivity.class);
            startActivity(intent);
        });

        googleBtn.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Google signup not implemented yet", Toast.LENGTH_SHORT).show();
        });

        return view;
    }

    private void registerUser(String name, String email, String password) {
        StringRequest request = new StringRequest(Request.Method.POST, REGISTER_URL,
                response -> {
                    response = response.trim();
                    if(response.equals("success")){
                        Toast.makeText(getActivity(), "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity(), loginActivity.class);
                        startActivity(intent);
                        getActivity().finish();
                    } else {
                        Toast.makeText(getActivity(), "Registration failed: " + response, Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(getActivity(), "Volley error: " + error.getMessage(), Toast.LENGTH_LONG).show()
        ){
            @Override
            protected Map<String, String> getParams(){
                Map<String, String> params = new HashMap<>();
                params.put("name", name);
                params.put("email", email);
                params.put("password", password);
                return params;
            }
        };

        Volley.newRequestQueue(getActivity()).add(request);
    }
}