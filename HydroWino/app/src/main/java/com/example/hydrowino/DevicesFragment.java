package com.example.hydrowino;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DevicesFragment extends Fragment {

    private TextView tvLabelWaterPump, tvLabelSolutionA, tvLabelSolutionB, tvLabelSolutionC;
    private TextView tvStatusWaterPump, tvStatusSolutionA, tvStatusSolutionB, tvStatusSolutionC;
    private TextView tvConnectionTitle, tvConnectionDesc;
    private View dotWaterPump, dotSolutionA, dotSolutionB, dotSolutionC;
    private View statusBadgeWaterPump, statusBadgeSolutionA, statusBadgeSolutionB, statusBadgeSolutionC;
    private View devicesContent, emptyStateLayout;
    
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference, connectionRef;
    private ValueEventListener motorListener, connectionListener;

    public DevicesFragment() {
        // Required empty public constructor
    }

    public static DevicesFragment newInstance() {
        return new DevicesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_devices, container, false);

        // Initialize Firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        
        devicesContent = view.findViewById(R.id.devicesContent);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        GreenhouseRepository.getInstance().setListener(data -> {
            if (!isAdded()) return;
            
            if (data != null && data.getGreenhouseID() != null) {
                String greenhouseId = data.getGreenhouseID();
                
                devicesContent.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);

                if (databaseReference == null || !databaseReference.toString().contains(greenhouseId)) {
                    // Remove old listeners
                    if (databaseReference != null && motorListener != null) databaseReference.removeEventListener(motorListener);
                    if (connectionRef != null && connectionListener != null) connectionRef.removeEventListener(connectionListener);
                    
                    databaseReference = firebaseDatabase.getReference("IoT").child(greenhouseId).child("devices");
                    connectionRef = firebaseDatabase.getReference("IoT").child(greenhouseId).child("status");
                    
                    // Re-attach listeners
                    GetDatamotor();
                    setupConnectionListener();
                }
            } else {
                devicesContent.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
        });

        // Initialize Labels
        tvLabelWaterPump = view.findViewById(R.id.tvSwitchLabelWaterPump);
        tvLabelSolutionA = view.findViewById(R.id.tvSwitchLabelSolutionA);
        tvLabelSolutionB = view.findViewById(R.id.tvSwitchLabelSolutionB);
        tvLabelSolutionC = view.findViewById(R.id.tvSwitchLabelSolutionC);

        // Initialize Status
        tvStatusWaterPump = view.findViewById(R.id.tvStatusWaterPump);
        tvStatusSolutionA = view.findViewById(R.id.tvStatusSolutionA);
        tvStatusSolutionB = view.findViewById(R.id.tvStatusSolutionB);
        tvStatusSolutionC = view.findViewById(R.id.tvStatusSolutionC);

        dotWaterPump = view.findViewById(R.id.dotWaterPump);
        dotSolutionA = view.findViewById(R.id.dotSolutionA);
        dotSolutionB = view.findViewById(R.id.dotSolutionB);
        dotSolutionC = view.findViewById(R.id.dotSolutionC);

        statusBadgeWaterPump = view.findViewById(R.id.statusWaterPump);
        statusBadgeSolutionA = view.findViewById(R.id.statusSolutionA);
        statusBadgeSolutionB = view.findViewById(R.id.statusSolutionB);
        statusBadgeSolutionC = view.findViewById(R.id.statusSolutionC);

        tvConnectionTitle = view.findViewById(R.id.tvConnectionTitle);
        tvConnectionDesc = view.findViewById(R.id.tvConnectionDesc);

        // Receive data from Firebase
        GetDatamotor();
        
        // Listen for connection status
        setupConnectionListener();

        return view;
    }

    private void setupConnectionListener() {
        if (connectionRef == null) return;
        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String statusStr = snapshot.getValue(String.class);
                boolean connected = "online".equalsIgnoreCase(statusStr);
                updateConnectionStatus(connected);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        connectionRef.addValueEventListener(connectionListener);
    }

    private void updateConnectionStatus(boolean connected) {
        String status = connected ? getString(R.string.status_online) : getString(R.string.status_offline);
        int textColor = connected ? ContextCompat.getColor(getContext(), R.color.status_improved_text) : ContextCompat.getColor(getContext(), R.color.text_color);
        int dotRes = connected ? R.drawable.ic_dot_green : R.drawable.ic_dot_gray;
        int bgRes = connected ? R.drawable.status_badge_bg : R.drawable.status_badge_offline_bg;

        updateSingleStatus(tvStatusWaterPump, dotWaterPump, statusBadgeWaterPump, status, textColor, dotRes, bgRes);
        updateSingleStatus(tvStatusSolutionA, dotSolutionA, statusBadgeSolutionA, status, textColor, dotRes, bgRes);
        updateSingleStatus(tvStatusSolutionB, dotSolutionB, statusBadgeSolutionB, status, textColor, dotRes, bgRes);
        updateSingleStatus(tvStatusSolutionC, dotSolutionC, statusBadgeSolutionC, status, textColor, dotRes, bgRes);

        if (tvConnectionTitle != null) {
            tvConnectionTitle.setText(connected ? getString(R.string.all_devices_online) : getString(R.string.all_devices_offline));
        }
        if (tvConnectionDesc != null) {
            tvConnectionDesc.setText(connected ? getString(R.string.connect_system_ready) : getString(R.string.connect_system_prompt));
        }
    }

    private void updateSingleStatus(TextView tv, View dot, View badge, String status, int textColor, int dotRes, int bgRes) {
        if (tv != null) {
            tv.setText(status);
            tv.setTextColor(textColor);
        }
        if (dot != null) {
            dot.setBackgroundResource(dotRes);
        }
        if (badge != null) {
            badge.setBackgroundResource(bgRes);
        }
    }

    private void GetDatamotor() {
        if (databaseReference == null) return;
        motorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                
                try {
                    // Handle Water Pump
                    Boolean waterPump = snapshot.child("waterPump").getValue(Boolean.class);
                    updateStatusLabel(tvLabelWaterPump, waterPump != null && waterPump);

                    // Handle Motors safely (works for both boolean and object structure)
                    updateStatusLabel(tvLabelSolutionA, isDeviceEnabled(snapshot.child("motor1")));
                    updateStatusLabel(tvLabelSolutionB, isDeviceEnabled(snapshot.child("motor2")));
                    updateStatusLabel(tvLabelSolutionC, isDeviceEnabled(snapshot.child("motor3")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        databaseReference.addValueEventListener(motorListener);
    }

    private boolean isDeviceEnabled(DataSnapshot snapshot) {
        if (!snapshot.exists()) return false;
        Object value = snapshot.getValue();
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (snapshot.child("enabled").exists()) {
            Boolean enabled = snapshot.child("enabled").getValue(Boolean.class);
            return enabled != null && enabled;
        }
        return false;
    }

    private void updateStatusLabel(TextView label, boolean isEnabled) {
        if (label != null) {
            label.setText(isEnabled ? getString(R.string.device_active) : getString(R.string.device_inactive));
        }
    }

    // setupSwitchListeners removed as switches are removed from UI

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseReference != null && motorListener != null) {
            databaseReference.removeEventListener(motorListener);
        }
        if (connectionRef != null && connectionListener != null) {
            connectionRef.removeEventListener(connectionListener);
        }
    }
}
