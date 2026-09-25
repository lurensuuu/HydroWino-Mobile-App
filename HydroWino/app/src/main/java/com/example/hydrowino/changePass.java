package com.example.hydrowino;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

public class changePass extends BaseActivity {

    private EditText etCurrentPass, etNewPass, etRePass;
    private Button btnSave;
    private TextView tvDescription, tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pass);

        mAuth = FirebaseAuth.getInstance();

        ImageView backLang = findViewById(R.id.backPass);
        etCurrentPass = findViewById(R.id.currenPass);
        etNewPass = findViewById(R.id.newPass);
        etRePass = findViewById(R.id.rePass);
        btnSave = findViewById(R.id.btnSave);
        tvDescription = findViewById(R.id.tvDescription);
        tvForgotPassword = findViewById(R.id.ForgotPassword);

        backLang.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> updatePassword());

        checkAccountProvider();
    }

    private void checkAccountProvider() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }

        boolean hasPasswordProvider = false;
        boolean hasGoogleProvider = false;

        // Iterate through linked authentication providers
        if (user.getProviderData() != null) {
            for (UserInfo profile : user.getProviderData()) {
                String providerId = profile.getProviderId();
                if (EmailAuthProvider.PROVIDER_ID.equals(providerId)) {
                    hasPasswordProvider = true;
                } else if (GoogleAuthProvider.PROVIDER_ID.equals(providerId)) {
                    hasGoogleProvider = true;
                }
            }
        }

        // If the account is purely Google managed (no password link associated)
        if (hasGoogleProvider && !hasPasswordProvider) {
            // Hide change password inputs and forgot password link
            etCurrentPass.setVisibility(View.GONE);
            etNewPass.setVisibility(View.GONE);
            etRePass.setVisibility(View.GONE);
            if (tvForgotPassword != null) tvForgotPassword.setVisibility(View.GONE);
            btnSave.setVisibility(View.GONE);

            // Display descriptive message
            tvDescription.setText("Your password is managed by your Google Account.");
            tvDescription.setTextColor(Color.RED);
        }
    }

    private void updatePassword() {
        String currentPassword = etCurrentPass.getText().toString().trim();
        String newPassword = etNewPass.getText().toString().trim();
        String rePassword = etRePass.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            etCurrentPass.setError(getString(R.string.err_current_pass_req));
            return;
        }

        if (TextUtils.isEmpty(newPassword)) {
            etNewPass.setError(getString(R.string.err_new_pass_req));
            return;
        }

        if (newPassword.length() < 6) {
            etNewPass.setError(getString(R.string.err_pass_length));
            return;
        }

        if (!newPassword.equals(rePassword)) {
            etRePass.setError(getString(R.string.err_pass_mismatch));
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            btnSave.setEnabled(false);
            btnSave.setText(getString(R.string.msg_updating));

            // Re-authenticate user before updating password
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Re-authentication successful, update password
                    user.updatePassword(newPassword).addOnCompleteListener(updateTask -> {
                        btnSave.setEnabled(true);
                        btnSave.setText(getString(R.string.update_password_btn));
                        if (updateTask.isSuccessful()) {
                            showCustomSnackbar(getString(R.string.success_title), getString(R.string.msg_pass_success), true);
                        } else {
                            Exception exception = updateTask.getException();
                            String errorMessage = "Failed to update password.";
                            if (exception instanceof FirebaseAuthWeakPasswordException) {
                                errorMessage = getString(R.string.err_pass_length);
                                etNewPass.setError(errorMessage);
                            } else if (exception instanceof FirebaseAuthRecentLoginRequiredException) {
                                errorMessage = "Security sensitive operation. Please log out and log back in to proceed.";
                            } else if (exception != null) {
                                errorMessage = exception.getMessage();
                            }
                            showCustomSnackbar(getString(R.string.error_title), errorMessage, false);
                        }
                    });
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText(getString(R.string.update_password_btn));
                    Exception exception = task.getException();
                    String errorMessage = getString(R.string.err_incorrect_pass);
                    
                    if (exception instanceof FirebaseNetworkException) {
                        errorMessage = "Network error. Please check your internet connection.";
                    } else {
                        etCurrentPass.setError(errorMessage);
                    }
                    showCustomSnackbar(getString(R.string.error_title), errorMessage, false);
                }
            });
        }
    }

    private void showCustomSnackbar(String title, String message, boolean isSuccess) {
        // Create snackbar anchored to etCurrentPass
        Snackbar snackbar = Snackbar.make(etCurrentPass, "", Snackbar.LENGTH_LONG);
        snackbar.setAnchorView(etCurrentPass); // This places it above the EditText

        View snackbarView = snackbar.getView();
        snackbarView.setBackgroundColor(Color.TRANSPARENT);

        LayoutInflater inflater = LayoutInflater.from(this);
        View customView = inflater.inflate(R.layout.layout_custom_snackbar, null);

        TextView tvTitle = customView.findViewById(R.id.tvTitle);
        TextView tvMessage = customView.findViewById(R.id.tvMessage);
        ImageView ivClose = customView.findViewById(R.id.ivClose);

        tvTitle.setText(title);
        tvMessage.setText(message);

        ivClose.setOnClickListener(v -> snackbar.dismiss());

        ViewGroup layout = (ViewGroup) snackbarView;
        layout.setPadding(0, 0, 0, 0);
        layout.addView(customView, 0);

        if (isSuccess) {
            snackbar.addCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar transientBottomBar, int event) {
                    finish();
                }
            });
        }

        snackbar.show();
    }
}
