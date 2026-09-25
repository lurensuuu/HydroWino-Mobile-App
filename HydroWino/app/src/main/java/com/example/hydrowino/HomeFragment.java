package com.example.hydrowino;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeFragment extends Fragment {

    // Slider
    private ViewPager2 viewPager;
    private SliderAdapter sliderAdapter;
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private int[] images = { R.drawable.background1, R.drawable.garden, R.drawable.hydrowino_logo };

    // Water Progress
    private CircularProgressIndicator waterProgress, temperatureProgress,humidityProgress, phProgress, tdsProgress, wtProgress;
    private TextView waterValue, waterStatus, temperatureValue, temperatureStatus,
            humidityValue, humidityStatus, phLevel, phStatus, tdsValue, tdsStatus, wtValue, wtStatus;

    private View emptyStateLayout, dashboardScrollView;
    private TextView tvPendingRequest;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    ValueEventListener sensorListener;


    // Runnable for auto slider
    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if(viewPager != null) {
                int next = (viewPager.getCurrentItem() + 1) % images.length;
                viewPager.setCurrentItem(next);
            }
        }
    };

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString("param1", param1);
        args.putString("param2", param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Image Slider Setup
        viewPager = view.findViewById(R.id.viewPager);
        DotsIndicator dots = view.findViewById(R.id.dotsIndicator);
        sliderAdapter = new SliderAdapter(images);
        viewPager.setAdapter(sliderAdapter);
        dots.setViewPager2(viewPager);

        // Auto slide callback
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000); // slide every 3 seconds
            }
        });


        // Water Progressbar, Value and Status
        waterProgress = view.findViewById(R.id.waterProgress);
        waterValue = view.findViewById(R.id.waterValue);
        waterStatus = view.findViewById(R.id.waterStatus);

        // water level (XL out of 40L)


        //Temperature Progressbar, Value and Status
        temperatureProgress = view.findViewById(R.id.temperatureProgress);
        temperatureValue = view.findViewById(R.id.temperatureValue);
        temperatureStatus = view.findViewById(R.id.temperatureStatus);



        //ph Progressbar, Value and Status
        phProgress = view.findViewById(R.id.phProgress);
        phLevel = view.findViewById(R.id.phValue);
        phStatus = view.findViewById(R.id.phStatus);

        tdsProgress = view.findViewById(R.id.tdsProgress);
        tdsValue = view.findViewById(R.id.tdsValue);
        tdsStatus = view.findViewById(R.id.tdsStatus);

        wtProgress = view.findViewById(R.id.WTProgress);
        wtValue = view.findViewById(R.id.WTValue);
        wtStatus = view.findViewById(R.id.WTStatus);

        humidityProgress = view.findViewById(R.id.humidityProgress);
        humidityValue = view.findViewById(R.id.humidityValue);
        humidityStatus = view.findViewById(R.id.humidityStatus);

        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        dashboardScrollView = view.findViewById(R.id.dashboardScrollView);
        tvPendingRequest = view.findViewById(R.id.tvPendingRequest);

        Button btnConnectGreenhouse = view.findViewById(R.id.btnConnectGreenhouse);
        btnConnectGreenhouse.setOnClickListener(v -> {
            // Open connect greenhouse activity
            android.content.Intent intent = new android.content.Intent(getContext(), ConnectGreenhouseActivity.class);
            startActivity(intent);
        });

        firebaseDatabase = FirebaseDatabase.getInstance();

        GreenhouseRepository.getInstance().setListener(new GreenhouseRepository.OnGreenhouseDataChangedListener() {
            @Override
            public void onDataChanged(GreenhouseRepository.GreenhouseData data) {
                if (!isAdded()) return;
                
                if (data != null && data.getGreenhouseID() != null) {
                    emptyStateLayout.setVisibility(View.GONE);
                    dashboardScrollView.setVisibility(View.VISIBLE);

                    // If the greenhouse ID changed or it's the first time
                    if (databaseReference == null || !databaseReference.toString().contains(data.getGreenhouseID())) {
                        if (databaseReference != null && sensorListener != null) {
                            databaseReference.removeEventListener(sensorListener);
                        }
                        databaseReference = firebaseDatabase.getReference("IoT").child(data.getGreenhouseID()).child("sensors");
                        GetData();
                    }
                } else {
                    dashboardScrollView.setVisibility(View.GONE);
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    tvPendingRequest.setVisibility(View.GONE);
                    btnConnectGreenhouse.setVisibility(View.VISIBLE); // Show button if no greenhouse and no pending request
                }
            }
        });

        return view;
    }

    private void GetData() {
        if (databaseReference == null) return;
        sensorListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;

                SensorData sensorData = snapshot.getValue(SensorData.class);
                if (sensorData != null) {
                    updateWaterLevel((int)sensorData.waterLevel, 40);
                    updateTemperature((int)sensorData.airTemp, 50);
                    updateHumidity((int)sensorData.humidity, 100);
                    updatePhLevel(sensorData.ph, 14.0);
                    updateTdsLevel(sensorData.tds, 2000);
                    updateWaterTemp((int)sensorData.waterTemp, 50);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        databaseReference.addValueEventListener(sensorListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (databaseReference != null && sensorListener != null) {
            databaseReference.removeEventListener(sensorListener);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }


    // Water Level Methods
    private void updateWaterLevel(int currentLiters, int maxLiters){
        Context context = getContext();
        if (context == null) return;

        int percent = (currentLiters * 100) / maxLiters;
        waterProgress.setProgress(percent, true);
        waterValue.setText(percent + "%");
        waterStatus.setText(currentLiters + "L remaining");

        if(percent >= 100) {
            waterProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            waterStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
            waterStatus.setText("Water Overflowing");
        } else if (percent >= 70) {
            waterProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.normal));
            waterStatus.setTextColor(ContextCompat.getColor(context, R.color.normal));
        } else if (percent >= 60) {
            waterProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.aboveNormal));
            waterStatus.setTextColor(ContextCompat.getColor(context, R.color.aboveNormal));
        } else {
            waterProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            waterStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
        }
    }

    private void updateTemperature(int currentTemp, int maxTemp){
        Context context = getContext();
        if (context == null) return;

        int percent = (currentTemp * 100) / maxTemp;
        temperatureProgress.setProgress(percent, true);
        temperatureValue.setText(currentTemp + "°C");

        if(currentTemp > 28) {
            temperatureProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            temperatureStatus.setText("Too Hot");
            temperatureStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
        } else if (currentTemp < 12) {
            temperatureProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.aboveNormal));
            temperatureStatus.setText("Too Cold");
            temperatureStatus.setTextColor(ContextCompat.getColor(context, R.color.aboveNormal));
        } else if((currentTemp >= 24 && currentTemp <=28)){
            temperatureProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.aboveNormal));
            temperatureStatus.setText("Above Normal");
            temperatureStatus.setTextColor(ContextCompat.getColor(context, R.color.aboveNormal));
        } else {
            temperatureProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.normal));
            temperatureStatus.setText("Normal");
            temperatureStatus.setTextColor(ContextCompat.getColor(context, R.color.normal));
        }
    }

    private void updateHumidity(int currentHumid, int maxHumid){
        Context context = getContext();
        if (context == null) return;

        int percent = (currentHumid * 100) / maxHumid;
        humidityProgress.setProgress(percent, true);
        humidityValue.setText(currentHumid+ "%");

        if(currentHumid >= 80){
            humidityProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            humidityStatus.setText("High Humidity");
            humidityStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
        } else if (currentHumid >= 50 && currentHumid <= 79) {
            humidityProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.normal));
            humidityStatus.setText("Normal");
            humidityStatus.setTextColor(ContextCompat.getColor(context, R.color.normal));
        } else {
            humidityProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            humidityStatus.setText("Low Humidity");
            humidityStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
        }
    }

    private void updatePhLevel(double currentPh, double maxPh){
        Context context = getContext();
        if (context == null) return;

        int percent = (int)((currentPh / maxPh) * 100);
        phProgress.setProgress(percent, true);
        phLevel.setText(String.format("%.1f", currentPh));

        if(currentPh < 5.5){
            phProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.aboveNormal));
            phStatus.setText("Too Acidic");
            phStatus.setTextColor(ContextCompat.getColor(context, R.color.aboveNormal));
        } else if(currentPh > 6.5){
            phProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            phStatus.setText("Too Alkaline");
            phStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
        } else {
            phProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.normal));
            phStatus.setText("Optimal");
            phStatus.setTextColor(ContextCompat.getColor(context, R.color.normal));
        }
    }

    private void updateTdsLevel(float currentTds, int maxTds) {
        Context context = getContext();
        if (context == null) return;

        int percent = (int)((currentTds * 100) / maxTds);
        tdsProgress.setProgress(percent, true);
        tdsValue.setText((int)currentTds + " ppm");

        if (currentTds > 1200) {
            tdsProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            tdsStatus.setText("High TDS");
            tdsStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
        } else if (currentTds < 500) {
            tdsProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.aboveNormal));
            tdsStatus.setText("Low TDS");
            tdsStatus.setTextColor(ContextCompat.getColor(context, R.color.aboveNormal));
        } else {
            tdsProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.normal));
            tdsStatus.setText("Normal");
            tdsStatus.setTextColor(ContextCompat.getColor(context, R.color.normal));
        }
    }

    private void updateWaterTemp(int currentTemp, int maxTemp) {
        Context context = getContext();
        if (context == null) return;

        int percent = (currentTemp * 100) / maxTemp;
        wtProgress.setProgress(percent, true);
        wtValue.setText(currentTemp + "°C");

        if (currentTemp > 28) {
            wtProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.notNormal));
            wtStatus.setText("Too Hot");
            wtStatus.setTextColor(ContextCompat.getColor(context, R.color.notNormal));
        } else if (currentTemp < 18) {
            wtProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.aboveNormal));
            wtStatus.setText("Too Cold");
            wtStatus.setTextColor(ContextCompat.getColor(context, R.color.aboveNormal));
        } else {
            wtProgress.setIndicatorColor(ContextCompat.getColor(context, R.color.normal));
            wtStatus.setText("Normal");
            wtStatus.setTextColor(ContextCompat.getColor(context, R.color.normal));
        }
    }
}
