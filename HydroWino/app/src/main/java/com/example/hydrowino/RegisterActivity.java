package com.example.hydrowino;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 9001;

    private EditText etfirstName, etlastName, etEmail, etPassword, etConfirmPassword;
    private TextInputLayout passwordInputLayout, confirmPasswordInputLayout;
    private Button signupBtn, googleBtn;
    private CheckBox termsCheckBox;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etfirstName = findViewById(R.id.etfirstName);
        etlastName = findViewById(R.id.etlastName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        passwordInputLayout = findViewById(R.id.passwordInputLayout);
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout);
        signupBtn = findViewById(R.id.SignupButton);
        googleBtn = findViewById(R.id.GoogleButton);
        termsCheckBox = findViewById(R.id.checkBox);
        TextView loginText = findViewById(R.id.HaveanAccount);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        loginText.setOnClickListener(v -> {
            startActivity(new Intent(this, loginActivity.class));
            finish();
        });

        signupBtn.setOnClickListener(v -> registerUser());
        googleBtn.setOnClickListener(v -> signInWithGoogle());
    }

    // ── Google Sign-Up ────────────────────────────────────────────────────────

    private void signInWithGoogle() {
        if (!termsCheckBox.isChecked()) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }
        googleBtn.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            googleBtn.setEnabled(true);
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Log.e("GoogleSignIn", "Error code: " + e.getStatusCode());
                Toast.makeText(this, "Google Sign-Up failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNewUser && user != null) {
                            // Extract first/last name from Google account
                            String fullName = account.getDisplayName() != null ? account.getDisplayName() : "";
                            String[] nameParts = fullName.split(" ", 2);
                            String firstName = nameParts.length > 0 ? nameParts[0] : "";
                            String lastName = nameParts.length > 1 ? nameParts[1] : "";
                            String email = account.getEmail() != null ? account.getEmail() : "";

                            // Save to Firestore
                            saveGoogleUserToFirestore(user.getUid(), firstName, lastName, email);
                        } else {
                            // Existing user — navigate to login or check status
                            startActivity(new Intent(this, loginActivity.class));
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveGoogleUserToFirestore(String userId, String firstName, String lastName, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("role", "cultivators");
        user.put("greenhouseID", null);
        user.put("createdAt", com.google.firebase.Timestamp.now());

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Welcome, " + firstName + "!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Removed checkUserApproval - not needed in new flow

    // ── Email/Password Register ───────────────────────────────────────────────

    private void registerUser() {
        String firstName = etfirstName.getText().toString().trim();
        String lastName = etlastName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Clear previous errors
        etfirstName.setError(null);
        etlastName.setError(null);
        etEmail.setError(null);
        passwordInputLayout.setError(null);
        confirmPasswordInputLayout.setError(null);

        if (TextUtils.isEmpty(firstName)) {
            etfirstName.setError("First Name is required.");
            return;
        }

        if (firstName.matches(".*[^a-zA-Z\\sñÑ.-].*")) {
            etfirstName.setError("First Name can only contain letters, spaces, dots, and hyphens.");
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            etlastName.setError("Last Name is required.");
            return;
        }

        if (lastName.matches(".*[^a-zA-Z\\sñÑ.-].*")) {
            etlastName.setError("Last Name can only contain letters, spaces, dots, and hyphens.");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required.");
            return;
        }

        // Strict Email Validation
        // Rules: No spaces, must have @, valid domain (e.g. .com, .ph), no multiple @,
        // cannot start/end with a dot, no invalid formats like juan@.com.
        String emailPattern = "^[a-zA-Z0-9]([a-zA-Z0-9._-]*[a-zA-Z0-9])?@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        if (!email.matches(emailPattern)) {
            etEmail.setError("Please enter a valid email address (e.g., user@example.com).");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInputLayout.setError("Password is required.");
            return;
        }

        // Password Strength Requirements:
        // Min 8, Max 64 chars, 1 Uppercase, 1 Lowercase, 1 Number, 1 Special Char
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!_\\-*/?<>.,:;()\\\\~`|\\[\\]{}])[A-Za-z\\d@#$%^&+=!_\\-*/?<>.,:;()\\\\~`|\\[\\]{}]{8,64}$";
        if (!password.matches(passwordPattern)) {
            passwordInputLayout.setError("Password must be 8-64 characters with uppercase, lowercase, number, and special character.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInputLayout.setError("Passwords do not match.");
            return;
        }

        if (!termsCheckBox.isChecked()) {
            Toast.makeText(this, "Please accept the terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        signupBtn.setEnabled(false);
        signupBtn.setText("Registering...");

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                saveUserToFirestore(user.getUid(), firstName, lastName, email);
                            }
                        } else {
                            // If registration fails, display a message to the user.
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                           // If registration fails, display a message to the user.
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(),Toast.LENGTH_LONG).show();
                            signupBtn.setEnabled(true);
                            signupBtn.setText("Sign up");
                        }
                    }
                });
    }

    private void saveUserToFirestore(String userId, String firstName, String lastName, String email) {
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("role", "cultivators");
        user.put("greenhouseID", null);
        user.put("createdAt", com.google.firebase.Timestamp.now());

        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(firstName + " " + lastName)
                    .build();
            firebaseUser.updateProfile(profileUpdates);
        }

        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    sendVerificationEmail();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    signupBtn.setEnabled(true);
                    signupBtn.setText("Sign up");
                });
    }

    private void sendVerificationEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                mAuth.signOut();
                                showSuccessSnackbar();
                            } else {
                                Toast.makeText(RegisterActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                signupBtn.setEnabled(true);
                                signupBtn.setText("Sign up");
                            }
                        }
                    });
        }
    }

    private void showSuccessSnackbar() {
        Snackbar snackbar = Snackbar.make(etfirstName, "", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAnchorView(etfirstName);

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.TRANSPARENT);

        LayoutInflater inflater = LayoutInflater.from(this);
        View customView = inflater.inflate(R.layout.layout_custom_snackbar, null);

        TextView tvTitle = customView.findViewById(R.id.tvTitle);
        TextView tvMessage = customView.findViewById(R.id.tvMessage);
        ImageView ivClose = customView.findViewById(R.id.ivClose);

        tvTitle.setText("Congratulations!");
        tvMessage.setText("Registration successful! Please check your email for verification.");

        ivClose.setOnClickListener(v -> {
            snackbar.dismiss();
            navigateToLogin();
        });

        ViewGroup layout = (ViewGroup) snackbarView;
        layout.setPadding(0, 0, 0, 0);
        layout.addView(customView, 0);

        new Handler().postDelayed(() -> {
            if (snackbar.isShown()) {
                snackbar.dismiss();
                navigateToLogin();
            }
        }, 5000);

        snackbar.show();
    }

    private void navigateToLogin() {
        startActivity(new Intent(RegisterActivity.this, loginActivity.class));
        finish();
    }

    // Removed createGreenhouseApplication and createInitialGreenhouse - not needed in new flow
}
