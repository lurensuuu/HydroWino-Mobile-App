package com.example.hydrowino;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {

    private FloatingActionButton fab;
    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private LinearLayout topBar;
    private TextView tvGreeting;
    private ImageView ivUserProfile;
    private View notificationBadge;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;

    // Register the permissions callback, which handles the user's response to the system permissions dialog.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your app.
                    openCamera();
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied.
                    Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.i("MainActivity", ">> APP STARTED - Initializing UI...");
        setContentView(R.layout.activity_main);

        // Initialize Notification Channel
        NotificationHelper.createChannel(this);

        // Request Permission for Android 13+
        checkNotificationPermission();

        // Start Background Monitoring Service
        Intent serviceIntent = new Intent(this, SensorMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Initialize views
        drawerLayout = findViewById(R.id.drawer_layout);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fab = findViewById(R.id.fab);

        topBar = findViewById(R.id.topBar);
        tvGreeting = findViewById(R.id.tvGreeting);
        ivUserProfile = findViewById(R.id.ivUserProfile);
        notificationBadge = findViewById(R.id.notificationBadge);

        ImageView ivMenu = findViewById(R.id.ivMenu);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Update Nav Header and Top Bar with user info
        updateUserInfo(navigationView);
        
        // Start Greenhouse Data Listener
        GreenhouseRepository.getInstance().startListening();
        listenForUnreadNotifications();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, PlantHistoryActivity.class));
            } else if (id == R.id.nav_settings) {
                // Handle settings
            } else if (id == R.id.nav_about) {
                // Handle about
            } else if (id == R.id.nav_logout) {
                // Sign out from Greenhouse Repository
                GreenhouseRepository.getInstance().logout();

                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();
                
                // Sign out from Google
                com.google.android.gms.auth.api.signin.GoogleSignInOptions gso = new com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut().addOnCompleteListener(this, task -> {
                    startActivity(new Intent(MainActivity.this, loginActivity.class));
                    finish();
                });
            }
            drawerLayout.closeDrawers();
            return true;
        });

        // Menu icon click to open drawer
        ivMenu.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.LEFT));

        // Notification icon click
        findViewById(R.id.notificationIconContainer).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        // Profile container click in top bar
        findViewById(R.id.profileContainer).setOnClickListener(v -> {
            bottomNavigationView.setSelectedItemId(R.id.profile);
        });

        // Default fragment
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
            showTopBar(true);
        }

        // Bottom navigation
        bottomNavigationView.setBackground(null);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Prevent re-selection of the same item to avoid redundant fragment transactions
            // and potential crashes due to rapid multiple commits.
            if (itemId == bottomNavigationView.getSelectedItemId()) {
                return true;
            }

            Fragment fragment = null;

            if (itemId == R.id.home) {
                fragment = new HomeFragment();
                showTopBar(true);
            }
            else if (itemId == R.id.devices) {
                fragment = new DevicesFragment();
                showTopBar(true);
            }
            else if (itemId == R.id.growth) {
                fragment = new GrowthFragment();
                showTopBar(true);
            }
            else if (itemId == R.id.profile) {
                fragment = new ProfileFragment();
                showTopBar(true);
            }

            if (fragment != null) {
                replaceFragment(fragment);
            }

            return true;
        });


        // FAB click
        fab.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // You can use the API that requires the permission.
                openCamera();
            } else {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });
    }

    private com.google.firebase.firestore.ListenerRegistration unreadNotificationsListener;

    private void listenForUnreadNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        GreenhouseRepository.getInstance().setListener(data -> {
            if (data != null && data.getGreenhouseID() != null) {
                String greenhouseId = data.getGreenhouseID();
                
                // Save to prefs so other legacy components stay in sync
                getSharedPreferences("HydroWinoPrefs", MODE_PRIVATE)
                        .edit().putString("greenhouse_id", greenhouseId).apply();

                // Remove old listener if exists
                if (unreadNotificationsListener != null) {
                    unreadNotificationsListener.remove();
                }

                unreadNotificationsListener = FirebaseFirestore.getInstance().collection("notification_logs")
                        .document(greenhouseId)
                        .collection("Logs")
                        .whereEqualTo("read", false)
                        .addSnapshotListener((value, error) -> {
                            if (error != null) return;
                            if (value != null && !value.isEmpty()) {
                                notificationBadge.setVisibility(View.VISIBLE);
                            } else {
                                notificationBadge.setVisibility(View.GONE);
                            }
                        });
            }
        });
    }

    private void updateUserInfo(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        TextView txtNavName = headerView.findViewById(R.id.txtName);
        TextView txtNavEmail = headerView.findViewById(R.id.txtEmail);
        ImageView imgNavProfile = headerView.findViewById(R.id.imgProfile);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Set basic info from Auth first
            if (user.getEmail() != null) {
                txtNavEmail.setText(user.getEmail());
            }

            String displayName = (user.getDisplayName() != null && !user.getDisplayName().isEmpty())
                    ? user.getDisplayName() : "User";

            txtNavName.setText(displayName);
            tvGreeting.setText(getString(R.string.greeting_hello) + displayName);

            // Load profile image from Auth first
            if (user.getPhotoUrl() != null) {
                loadProfileImage(user.getPhotoUrl().toString(), imgNavProfile, ivUserProfile);
            }

            // Fetch detailed info from Firestore for accuracy
            FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String firstName = document.getString("firstName");
                                String lastName = document.getString("lastName");
                                String profileImageUrl = document.getString("profileImageUrl");

                                String fullName = "";
                                if (firstName != null && lastName != null) {
                                    fullName = firstName + " " + lastName;
                                } else if (firstName != null) {
                                    fullName = firstName;
                                }

                                if (!fullName.isEmpty()) {
                                    txtNavName.setText(fullName);
                                    tvGreeting.setText(getString(R.string.greeting_hello) + firstName);
                                }

                                // Load profile image from Firestore if available
                                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                    loadProfileImage(profileImageUrl, imgNavProfile, ivUserProfile);
                                }
                            }
                        }
                    });
        }
    }

    private void loadProfileImage(String url, ImageView... imageViews) {
        for (ImageView imageView : imageViews) {
            if (imageView != null) {
                Glide.with(this)
                        .load(url)
                        .apply(RequestOptions.circleCropTransform())
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(imageView);
            }
        }
    }

    private void openCamera() {
        Intent intent = new Intent(this, CameraActivity.class);
        startActivity(intent);
    }

    // Replace fragments
    private void replaceFragment(Fragment fragment) {
        if (isFinishing()) return;
        
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.isStateSaved()) return;

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    // Show bottom dialog
    private void showBottomDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout videoLayout = dialog.findViewById(R.id.layoutVideo);
        LinearLayout shortsLayout = dialog.findViewById(R.id.layoutShorts);
        LinearLayout liveLayout = dialog.findViewById(R.id.layoutLive);
        ImageView cancelButton = dialog.findViewById(R.id.cancelButton);

        videoLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity.this,"Upload a Video clicked",Toast.LENGTH_SHORT).show();
        });

        shortsLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity.this,"Create a Short clicked",Toast.LENGTH_SHORT).show();
        });

        liveLayout.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(MainActivity.this,"Go Live clicked",Toast.LENGTH_SHORT).show();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            }
        }
    }
    // Show or hide top bar
    private void showTopBar(boolean show) {
        if (show) {
            topBar.setVisibility(View.VISIBLE);
        } else {
            topBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
        }
    }
}
