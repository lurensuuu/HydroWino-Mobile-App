package com.example.hydrowino;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ProfileFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    private ImageView profileImage;
    private ImageView editIcon;
    private TextView tvName, tvEmail;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;

    private ActivityResultLauncher<String> mGetContent;
    private Uri currentPhotoUrl;

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance(String param1, String param2) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadImageToFirebase(uri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileImage = view.findViewById(R.id.profileImage);
        editIcon = view.findViewById(R.id.editIcon);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvName = view.findViewById(R.id.tvName);

        TextView menuLogout = view.findViewById(R.id.menuLogout);
        TextView menuLanguage = view.findViewById(R.id.menuLanguage);
        TextView menuSecurity = view.findViewById(R.id.menuSecurity);
        TextView menuDisplay = view.findViewById(R.id.menuDisplay);
        TextView menuNotification = view.findViewById(R.id.menuNotifications);
        TextView menuGrowth = view.findViewById(R.id.menuGrowth);
        TextView menuGreenhouse = view.findViewById(R.id.menuGreenhouse);
        TextView menuBatchHistory = view.findViewById(R.id.myPlants);

        // Load current user data
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            loadUserData(user);
        }

        editIcon.setOnClickListener(v -> mGetContent.launch("image/*"));

        profileImage.setOnClickListener(v -> {
            if (currentPhotoUrl != null) {
                showFullImage(currentPhotoUrl);
            } else {
                // If no image, allow picking one
                mGetContent.launch("image/*");
            }
        });
        // Fetch from Firestore "users" collection (since we switched to Firestore)
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String firstName = document.getString("firstName");
                            String lastName = document.getString("lastName");
                            if (firstName != null && lastName != null) {
                                tvName.setText(firstName + " " + lastName);
                            } else if (firstName != null) {
                                tvName.setText(firstName);
                            }
                        }
                    }
                });

        menuBatchHistory.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PlantHistoryActivity.class);
            startActivity(intent);
        });

        menuNotification.setOnClickListener(v -> {
        Intent intent = new Intent(requireContext(), NotificationsActivity.class);
        startActivity(intent);
    });

        menuLanguage.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Language.class);
            startActivity(intent);
        });

        menuSecurity.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PasswordandSecurity.class);
            startActivity(intent);
        });

        menuDisplay.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), display.class);
            startActivity(intent);
        });

        menuGrowth.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), GrowthDetailsActivity.class);
            startActivity(intent);
        });

        menuGreenhouse.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), GreenhouseActivity.class);
            startActivity(intent);
        });


        menuLogout.setOnClickListener(v -> {

            AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext(), R.style.CustomAlertDialog)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialogInterface, which) -> {
                        // Sign out from Firebase
                        mAuth.signOut();
                        
                        // Sign out from Google
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build();
                        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(requireContext(), gso).signOut().addOnCompleteListener(task -> {
                            SharedPreferences prefs = requireContext()
                                    .getSharedPreferences("user_session", Context.MODE_PRIVATE);
                            prefs.edit().clear().apply();

                            Intent intent = new Intent(requireContext(), loginActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .create();

            dialog.show();

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color));

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_color));

            int width = (int) (350 * getResources().getDisplayMetrics().density);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);

        });
    }

    private void showFullImage(Uri imageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_full_image, null);
        ImageView fullImageView = dialogView.findViewById(R.id.fullImageView);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        Glide.with(this)
                .load(imageUri)
                .into(fullImageView);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void loadUserData(FirebaseUser user) {
        if (user.getEmail() != null) {
            tvEmail.setText(user.getEmail());
        }

        // Load profile image using Glide for circular cropping
        if (user.getPhotoUrl() != null) {
            currentPhotoUrl = user.getPhotoUrl();
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder)
                    .into(profileImage);
        }

        // Fetch name and other details from Firestore
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isAdded()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            String firstName = document.getString("firstName");
                            String lastName = document.getString("lastName");
                            String profileImageUrl = document.getString("profileImageUrl");

                            if (firstName != null && lastName != null) {
                                tvName.setText(firstName + " " + lastName);
                            }

                            // If Auth photoUrl is null but Firestore has it, load it
                            if (user.getPhotoUrl() == null && profileImageUrl != null) {
                                currentPhotoUrl = Uri.parse(profileImageUrl);
                                Glide.with(this)
                                        .load(profileImageUrl)
                                        .apply(RequestOptions.circleCropTransform())
                                        .placeholder(R.drawable.profile_placeholder)
                                        .into(profileImage);
                            }
                        }
                    }
                });
    }

    private void uploadImageToFirebase(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Toast.makeText(getContext(), "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        try {
            // Load and compress the image
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            // Resize the bitmap to avoid very large files
            int maxSize = 1024;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            if (width > maxSize || height > maxSize) {
                float ratio = (float) width / height;
                if (width > height) {
                    width = maxSize;
                    height = (int) (width / ratio);
                } else {
                    height = maxSize;
                    width = (int) (height * ratio);
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            // Convert bitmap to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] data = baos.toByteArray();

            StorageReference fileRef = storageReference.child("profile_pictures/" + user.getUid() + ".jpg");

            fileRef.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        currentPhotoUrl = uri;

                        // 1. Update Firebase Auth Profile
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setPhotoUri(uri)
                                .build();

                        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                            if (task.isSuccessful() && isAdded()) {
                                // 2. Update Firestore
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("profileImageUrl", downloadUrl);

                                db.collection("users").document(user.getUid())
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            if (isAdded()) {
                                                Glide.with(ProfileFragment.this)
                                                        .load(downloadUrl)
                                                        .apply(RequestOptions.circleCropTransform())
                                                        .into(profileImage);
                                                Toast.makeText(getContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        });
                    }))
                    .addOnFailureListener(e -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (IOException e) {
            Toast.makeText(getContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}