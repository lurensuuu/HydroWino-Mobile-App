package com.example.hydrowino;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class GrowthDetailsActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private View layoutEmptyState;
    private GrowthRecordAdapter adapter;
    private List<Object> displayList;
    private List<WeeklyRecord> weeklyRecords;
    private FirebaseFirestore db;
    private TextView tvTitle;
    private boolean isWeeklyView = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_growth_details);

        tvTitle = findViewById(R.id.textViewTitle);
        if (tvTitle == null) {
            tvTitle = (TextView) ((android.view.ViewGroup)findViewById(R.id.changePassbt)).getChildAt(1);
        }

        ImageView backBtn = findViewById(R.id.backPass);
        backBtn.setOnClickListener(v -> handleBack());

        recyclerView = findViewById(R.id.recyclerViewGrowth);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        displayList = new ArrayList<>();
        weeklyRecords = new ArrayList<>();
        adapter = new GrowthRecordAdapter(displayList, item -> {
            if (item instanceof WeeklyRecord) {
                showDailyView((WeeklyRecord) item);
            } else if (item instanceof GrowthRecord) {
                showPlantMeasurements((GrowthRecord) item);
            }
        });

        db = FirebaseFirestore.getInstance();

        GreenhouseRepository.getInstance().setListener(data -> {
            if (data != null) {
                adapter.setMetadata(data.getGreenhouseID(), data.getCurrentBatch());
                fetchGrowthData(data);
            }
        });

        recyclerView.setAdapter(adapter);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (!isWeeklyView) {
                    showWeeklyView();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void handleBack() {
        if (!isWeeklyView) {
            showWeeklyView();
        } else {
            finish();
        }
    }

    private void showWeeklyView() {
        isWeeklyView = true;
        tvTitle.setText(getString(R.string.growth_details));
        displayList.clear();
        displayList.addAll(weeklyRecords);
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (displayList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
        }
    }

    private void showDailyView(WeeklyRecord week) {
        isWeeklyView = false;
        tvTitle.setText(getString(R.string.week) + " " + week.getWeekNumber());
        displayList.clear();
        displayList.addAll(week.getDailyRecords());
        // Sort days descending
        Collections.sort(displayList, (o1, o2) -> {
            GrowthRecord r1 = (GrowthRecord) o1;
            GrowthRecord r2 = (GrowthRecord) o2;
            return r2.getDate().compareTo(r1.getDate());
        });
        adapter.notifyDataSetChanged();
    }

    private void showPlantMeasurements(GrowthRecord record) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.dialog_plant_measurements, null);

        TextView tvSubtitle = bottomSheetView.findViewById(R.id.tvSubtitle);
        RecyclerView rvPlants = bottomSheetView.findViewById(R.id.rvPlants);
        TextView tvEmptyState = bottomSheetView.findViewById(R.id.tvEmptyState);

        String subtitle = (record.getDay() != null ? record.getDay() : "Day") + " • " + record.getDate();
        tvSubtitle.setText(subtitle);

        GrowthRecord.DailyAverage avg = record.getDailyAverage();
        if (avg != null && avg.getPlants() != null && !avg.getPlants().isEmpty()) {
            List<GrowthRecord.PlantMeasurement> plants = new ArrayList<>(avg.getPlants());
            // Sort plants by plantId ascending
            Collections.sort(plants, (p1, p2) -> Long.compare(p1.getPlantId(), p2.getPlantId()));

            rvPlants.setLayoutManager(new LinearLayoutManager(this));
            rvPlants.setAdapter(new PlantMeasurementAdapter(plants));
            rvPlants.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        } else {
            rvPlants.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        }

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    private void fetchGrowthData(GreenhouseRepository.GreenhouseData ghData) {
        long plantingStart = ghData.getPlantingStartTime() != null ? ghData.getPlantingStartTime().toDate().getTime() : 0;
        String currentBatch = ghData.getCurrentBatch();
        String greenhouseId = ghData.getGreenhouseID();

        db.collection("PlantGrowthData")
                .document(greenhouseId)
                .collection("Batches")
                .document(currentBatch)
                .collection("Records")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<GrowthRecord> allDailyRecords = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        GrowthRecord record = document.toObject(GrowthRecord.class);
                        String dateStr = document.getId();
                        record.setDate(dateStr);
                        allDailyRecords.add(record);

                        db.collection("PlantGrowthData")
                                .document(greenhouseId)
                                .collection("Batches")
                                .document(currentBatch)
                                .collection("Records")
                                .document(dateStr)
                                .collection("OneHourlyRecords")
                                .orderBy("timestamp")
                                .get()
                                .addOnSuccessListener(hourlySnapshots -> {
                                    if (hourlySnapshots != null && !hourlySnapshots.isEmpty()) {
                                        android.util.Log.d("HydroWinoHourlyReader",
                                                "Greenhouse = " + greenhouseId + "\n" +
                                                "Batch = " + currentBatch + "\n" +
                                                "Date = " + dateStr + "\n" +
                                                "Hourly records loaded = " + hourlySnapshots.size());
                                    } else {
                                        android.util.Log.d("HydroWinoHourlyReader",
                                                "No hourly records available for " + dateStr);
                                    }
                                });
                    }

                    processDataIntoWeeks(allDailyRecords, plantingStart);
                    showWeeklyView();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void processDataIntoWeeks(List<GrowthRecord> records, long plantingStart) {
        weeklyRecords.clear();
        if (records.isEmpty()) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        
        // Normalize plantingStart to the beginning of that day
        long normalizedStart = 0;
        if (plantingStart != 0) {
            try {
                String startDateStr = sdf.format(new Date(plantingStart));
                Date startDate = sdf.parse(startDateStr);
                if (startDate != null) normalizedStart = startDate.getTime();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // If still 0, we'll try to determine it from the earliest record later if needed
        Map<Integer, WeeklyRecord> weekMap = new TreeMap<>(Collections.reverseOrder());

        for (GrowthRecord record : records) {
            try {
                GrowthRecord.DailyAverage avg = record.getDailyAverage();
                int dayNumber = -1;

                // 1. First, try to use the 'day' field already in Firebase if it exists
                if (avg != null && avg.getDay() != null && !avg.getDay().isEmpty()) {
                    try {
                        String numericPart = avg.getDay().replaceAll("[^0-9]", "");
                        if (!numericPart.isEmpty()) {
                            dayNumber = Integer.parseInt(numericPart);
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                // 2. Fallback to calculation if dayNumber is still unknown
                if (dayNumber <= 0) {
                    Date recordDate = sdf.parse(record.getDate());
                    if (recordDate != null) {
                        if (normalizedStart == 0) {
                            // If no start date, the first record we encounter (after sorting) should define Day 1
                            // But we haven't sorted the whole list yet in a reliable way for this fallback
                            // Let's just use the current record's date as a temporary start if none exists
                            normalizedStart = recordDate.getTime();
                        }
                        long diff = recordDate.getTime() - normalizedStart;
                        dayNumber = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
                    }
                }

                if (dayNumber >= 1) {
                    // Update labels
                    String dayLabel = getString(R.string.day) + " " + dayNumber;
                    record.setDay(dayLabel);
                    if (avg != null) avg.setDay(dayLabel);

                    // Week calculation: 1-7 -> W1, 8-14 -> W2, etc.
                    int weekNumber = (dayNumber - 1) / 7 + 1;
                    
                    WeeklyRecord weekly = weekMap.get(weekNumber);
                    if (weekly == null) {
                        weekly = new WeeklyRecord(weekNumber);
                        weekMap.put(weekNumber, weekly);
                    }
                    weekly.addDailyRecord(record);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        weeklyRecords.clear();
        for (WeeklyRecord weekly : weekMap.values()) {
            weekly.calculateAverages();
            weeklyRecords.add(weekly);
        }
    }
}
