package com.example.hydrowino;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GrowthFragment extends Fragment {

    private TooltipCombinedChart chart;
    private FirebaseFirestore db;
    private float[] loadedValues;
    private boolean isHarvested = false;
    private String currentGreenhouseID;
    private String currentBatchName;

    private TextView tvDayValue, tvWeeklyTrendValue, tvWeeklyTrendDesc;
    private TextView tvTotalDays, tvProgressPercentage;
    private TextView tvWeeklyPercentBadge, tvVsLastWeek, tvWeeklyTrendDetailedDesc;
    private ImageView ivWeeklyIcon, ivTrendArrow;
    private View layoutWeeklyBottomPanel;
    private TextView tvDailyTrendTag, tvRecommendationTitle;
    private Button btnHarvest;
    
    // Daily Recommendation views
    private TextView tvResultTitle, tvResultDescription, tvRecommendationMessage;
    private ImageView ivResultIcon;
    private View recommendationCard;

    // Weekly Recommendation views
    private TextView tvWeeklyResultTitle, tvWeeklyResultDescription;
    private TextView tvWeeklyTrendStatus, tvWeeklyBadgeIcon, tvWeeklyBadgeText;
    private View weeklyRecommendationCard, layoutWeeklyBadge;

    // Cause and Action Layouts
    private LinearLayout layoutCauses, layoutActions;
    private View cardCauses, cardActions;
    private View growthContent, emptyStateLayout;

    private String[] dayLabels = {"-","-","-","-","-","-","-"};
    private int highlightIndex = 6; // Default to last day

    public GrowthFragment() {}

    private GreenhouseRepository.OnGreenhouseDataChangedListener greenhouseListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_growth, container, false);

        semiCircleProgress semi = view.findViewById(R.id.semiProgress);
        semi.setMax(30);
        semi.setProgress(0); // Initialize

        chart = view.findViewById(R.id.lineChart);
        db    = FirebaseFirestore.getInstance();

        tvDayValue = view.findViewById(R.id.dayValue);
        tvTotalDays = view.findViewById(R.id.tvTotalDays);
        tvProgressPercentage = view.findViewById(R.id.tvProgressPercentage);
        
        tvWeeklyTrendValue = view.findViewById(R.id.tvWeeklyTrendValue);
        tvWeeklyTrendDesc = view.findViewById(R.id.tvWeeklyTrendDesc);
        tvWeeklyPercentBadge = view.findViewById(R.id.tvWeeklyPercentBadge);
        tvVsLastWeek = view.findViewById(R.id.tvVsLastWeek);
        tvWeeklyTrendDetailedDesc = view.findViewById(R.id.tvWeeklyTrendDetailedDesc);
        ivWeeklyIcon = view.findViewById(R.id.ivWeeklyIcon);
        ivTrendArrow = view.findViewById(R.id.ivTrendArrow);
        layoutWeeklyBottomPanel = view.findViewById(R.id.layoutWeeklyBottomPanel);

        tvDailyTrendTag = view.findViewById(R.id.tvDailyTrendTag);
        tvRecommendationTitle = view.findViewById(R.id.tvRecommendationTitle);
        btnHarvest = view.findViewById(R.id.btnHarvest);
        
        // Setup daily card
        recommendationCard = view.findViewById(R.id.recommendationCard);
        ivResultIcon = view.findViewById(R.id.ivResultIcon);
        tvResultTitle = view.findViewById(R.id.tvResultTitle);
        tvResultDescription = view.findViewById(R.id.tvResultDescription);
        tvRecommendationMessage = view.findViewById(R.id.tvRecommendationMessage);

        // Setup weekly card
        weeklyRecommendationCard = view.findViewById(R.id.weeklyRecommendationCard);
        tvWeeklyResultTitle = view.findViewById(R.id.tvWeeklyResultTitle);
        tvWeeklyResultDescription = view.findViewById(R.id.tvWeeklyResultDescription);
        tvWeeklyTrendStatus = view.findViewById(R.id.tvWeeklyTrendStatus);
        tvWeeklyBadgeIcon = view.findViewById(R.id.tvWeeklyBadgeIcon);
        tvWeeklyBadgeText = view.findViewById(R.id.tvWeeklyBadgeText);
        layoutWeeklyBadge = view.findViewById(R.id.layoutWeeklyBadge);

        // Setup Cause and Action cards
        layoutCauses = view.findViewById(R.id.layoutCauses);
        layoutActions = view.findViewById(R.id.layoutActions);
        cardCauses = view.findViewById(R.id.cardCauses);
        cardActions = view.findViewById(R.id.cardActions);
        
        growthContent = view.findViewById(R.id.growthContent);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        Button btnDetails = view.findViewById(R.id.btnDetails);
        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), GrowthDetailsActivity.class);
            startActivity(intent);
        });

        setupChartAppearance();

        btnHarvest.setOnClickListener(v -> {
            if (isHarvested) {
                startNewPlanting();
            } else {
                new AlertDialog.Builder(getContext())
                        .setTitle("Confirm Harvest")
                        .setMessage("Are you sure you want to harvest your plants now? This will finalize the current batch and save it to history.")
                        .setPositiveButton("Yes, Harvest", (dialog, which) -> harvestPlants())
                        .setNegativeButton("Not yet", null)
                        .show();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.d("GrowthFragment", "onViewCreated: Setting up repository listener");
        
        greenhouseListener = data -> {
            if (!isAdded()) return;
            
            if (data != null && getView() != null) {
                Log.d("GrowthFragment", "Data received: Batch=" + data.getCurrentBatch() + " Harvested=" + data.isHarvested());
                this.isHarvested = data.isHarvested();
                this.currentGreenhouseID = data.getGreenhouseID();
                this.currentBatchName = data.getCurrentBatch();
                
                growthContent.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                
                updateHarvestButtonUI();
                loadChartDataFromFirestore();
                analyzeGrowthData();
            } else {
                Log.d("GrowthFragment", "No greenhouse data, showing empty state");
                growthContent.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
        };
        
        GreenhouseRepository.getInstance().setListener(greenhouseListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (greenhouseListener != null) {
            GreenhouseRepository.getInstance().removeListener(greenhouseListener);
        }
    }

    private void updateHarvestButtonUI() {
        if (isHarvested) {
            btnHarvest.setText("Start Planting");
            btnHarvest.setVisibility(View.VISIBLE);
            btnHarvest.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.primary_color)));
        } else {
            btnHarvest.setText("You are ready to harvest");
            // Visibility is handled in analyzeGrowthData based on dayNum
        }
    }

    private void harvestPlants() {
        GreenhouseRepository.GreenhouseData data = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (data == null) return;

        String currentBatch = data.getCurrentBatch();
        String greenhouseId = data.getGreenhouseID();
        long plantingStart = data.getPlantingStartTime() != null ? data.getPlantingStartTime().toDate().getTime() : 0;
        
        // Fetch data summary for the batch before resetting
        db.collection("PlantGrowthData")
                .document(greenhouseId)
                .collection("Batches")
                .document(currentBatch)
                .collection("Records")
                .orderBy("DailyBestCondition.timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    double totalGrowthValue = 0, totalTempValue = 0, totalHumidValue = 0, totalPhValue = 0;
                    int count = queryDocumentSnapshots.size();
                    List<Float> sparkline = new ArrayList<>();
                    String lastDate = "";
                    String firstDate = "";

                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();
                        Collections.reverse(docs); // Order from oldest to newest for range and sparkline
                        
                        firstDate = docs.get(0).getId();
                        lastDate = docs.get(docs.size() - 1).getId();

                        for (DocumentSnapshot doc : docs) {
                            double g = getAvgGrowth(doc);
                            totalGrowthValue += g;
                            totalTempValue += getAvgTemp(doc);
                            totalHumidValue += getAvgHumid(doc);
                            totalPhValue += getAvgPh(doc);
                            sparkline.add((float) g);
                        }
                    }

                    // Prepare History Entry
                    Map<String, Object> history = new HashMap<>();
                    history.put("name", currentBatch);
                    history.put("status", "Completed ✓");
                    history.put("type", "Hydroponic Plant"); // Or fetch from settings if available
                    history.put("dateRange", firstDate + " - " + lastDate);
                    
                    int dayCount = count;
                    if (plantingStart > 0) {
                        long diff = System.currentTimeMillis() - plantingStart;
                        dayCount = (int) (diff / (1000 * 60 * 60 * 24)) + 1;
                    }
                    history.put("duration", dayCount + " days");
                    
                    history.put("avgTemp", count > 0 ? totalTempValue / count : 0);
                    history.put("avgPH", count > 0 ? totalPhValue / count : 0);
                    history.put("avgHumidity", count > 0 ? totalHumidValue / count : 0);
                    history.put("avgGrowth", (count > 0 ? String.format(Locale.getDefault(), "%.1f cm", totalGrowthValue / count) : "0 cm"));
                    history.put("sparkLineData", sparkline);
                    history.put("greenhouseId", greenhouseId);
                    history.put("timestamp", com.google.firebase.Timestamp.now());

                    db.collection("PlantHistory").add(history)
                            .addOnSuccessListener(docRef -> {
                                Log.d("GrowthFragment", "History saved: " + docRef.getId());
                                executeHarvestReset();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("GrowthFragment", "Error saving history", e);
                                executeHarvestReset(); // Still reset even if history fails
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("GrowthFragment", "Error fetching batch data", e);
                    executeHarvestReset();
                });
    }

    private void executeHarvestReset() {
        if (!isAdded()) return;
        isHarvested = true;
        GreenhouseRepository.getInstance().updateHarvestStatus(true);
        if (getContext() != null) {
            Toast.makeText(getContext(), "Congratulations! Your plants are ready for harvest.", Toast.LENGTH_LONG).show();
        }
        
        // Reset UI
        tvDayValue.setText("0");
        if (tvTotalDays != null) tvTotalDays.setText("of 30");
        if (tvProgressPercentage != null) tvProgressPercentage.setText("0% Complete");
        tvWeeklyTrendValue.setText("0.0 cm");
        if (tvWeeklyPercentBadge != null) tvWeeklyPercentBadge.setText("— 0%");
        if (tvVsLastWeek != null) tvVsLastWeek.setText("vs last week (0.0 cm)");
        tvWeeklyTrendDesc.setText("Harvested");
        tvResultDescription.setText("All plants harvested.");
        tvRecommendationMessage.setText("Ready for next batch.");
        cardCauses.setVisibility(View.GONE);
        cardActions.setVisibility(View.GONE);

        View currentView = getView();
        if (currentView != null) {
            semiCircleProgress semi = currentView.findViewById(R.id.semiProgress);
            if (semi != null) semi.setProgress(0);
        }
        
        updateHarvestButtonUI();
    }

    private void startNewPlanting() {
        GreenhouseRepository.GreenhouseData data = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (data == null) return;

        isHarvested = false;
        
        // Increment Batch
        String currentBatch = data.getCurrentBatch() != null ? data.getCurrentBatch() : "Batch 0";
        int batchNum = 1;
        try {
            batchNum = Integer.parseInt(currentBatch.replace("Batch ", "")) + 1;
        } catch (Exception ignored) {}
        String nextBatch = "Batch " + batchNum;
        
        GreenhouseRepository.getInstance().updateCurrentBatch(nextBatch);
        GreenhouseRepository.getInstance().updateHarvestStatus(false);
        
        Toast.makeText(getContext(), "New planting started: " + nextBatch, Toast.LENGTH_SHORT).show();
        
        updateHarvestButtonUI();
        
        // Reload data for new batch
        loadChartDataFromFirestore();
        analyzeGrowthData();
    }

    private void analyzeGrowthData() {
        GreenhouseRepository.GreenhouseData data = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (data == null) {
            Log.w("GrowthFragment", "analyzeGrowthData: Greenhouse data is null, skipping update");
            return;
        }

        Log.d("GrowthFragment", "analyzeGrowthData: Updating UI for Batch=" + data.getCurrentBatch());

        if (isHarvested) {
            tvDayValue.setText("0");
            btnHarvest.setVisibility(View.VISIBLE);
            tvWeeklyTrendValue.setText("0.0 cm");
            if (tvWeeklyPercentBadge != null) tvWeeklyPercentBadge.setText("— 0%");
            if (tvVsLastWeek != null) tvVsLastWeek.setText("vs last week (0.0 cm)");
            tvWeeklyTrendDesc.setText("Harvested");
            tvResultDescription.setText("All plants harvested.");
            tvRecommendationMessage.setText("Ready for next batch.");
            cardCauses.setVisibility(View.GONE);
            cardActions.setVisibility(View.GONE);
            return;
        }

        String currentBatch = data.getCurrentBatch();
        String greenhouseId = data.getGreenhouseID();
        
        Log.d("GrowthFragment", "analyzeGrowthData: Querying Records for Greenhouse=" + greenhouseId + " Batch=" + currentBatch);

        db.collection("PlantGrowthData")
                .document(greenhouseId)
                .collection("Batches")
                .document(currentBatch)
                .collection("Records")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    List<DocumentSnapshot> docs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                    Collections.sort(docs, (d1, d2) -> d2.getId().compareTo(d1.getId()));
                    if (docs.size() > 14) {
                        docs = docs.subList(0, 14);
                    }
                    Log.d("GrowthFragment", "analyzeGrowthData: Fetched " + docs.size() + " records");
                    for (DocumentSnapshot d : docs) {
                        Log.d("GrowthFragment", "analyzeGrowthData: Found record document: " + d.getId() + " Growth=" + getAvgGrowth(d));
                        
                        // Read hourly records created by the website Admin/backend
                        String dateStr = d.getId();
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
                    if (docs.isEmpty()) {
                        tvResultDescription.setText("No data recorded yet.");
                        tvDayValue.setText("0");
                        cardCauses.setVisibility(View.GONE);
                        cardActions.setVisibility(View.GONE);
                        if (btnHarvest != null && !isHarvested) {
                            btnHarvest.setVisibility(View.GONE);
                        }
                        return;
                    }

                    DocumentSnapshot latestDoc = docs.get(0);
                    String dayStr = getDayString(latestDoc);
                    int dayNum = parseDayNumber(dayStr);

                    Log.d("GrowthFragment", "greenhouseId = " + greenhouseId);
                    Log.d("GrowthFragment", "currentBatch = " + currentBatch);
                    Log.d("GrowthFragment", "recordsFound = " + docs.size());
                    Log.d("GrowthFragment", "latestRecord = " + latestDoc.getId());
                    Log.d("GrowthFragment", "latestDayRaw = " + dayStr);
                    Log.d("GrowthFragment", "latestDayNumber = " + dayNum);

                    if (dayNum == 0 && !docs.isEmpty()) {
                        Log.w("GrowthFragment", "DailyBestCondition.day missing for record " + latestDoc.getId());
                    }

                    tvDayValue.setText(String.valueOf(dayNum));

                    if (tvTotalDays != null) tvTotalDays.setText("of 30");
                    if (tvProgressPercentage != null) {
                        float percent = (dayNum / 30f) * 100;
                        if (percent > 100) percent = 100;
                        tvProgressPercentage.setText(String.format(Locale.getDefault(), "%.1f%% Complete", percent));
                    }

                    if (btnHarvest != null && !isHarvested) {
                        if (dayNum >= 26) {
                            btnHarvest.setVisibility(View.VISIBLE);
                        } else {
                            btnHarvest.setVisibility(View.GONE);
                        }
                    }
                    
                    View currentView = getView();
                    if (currentView != null) {
                        semiCircleProgress semi = currentView.findViewById(R.id.semiProgress);
                        if (semi != null) {
                            semi.setMax(30);
                            semi.setProgress(dayNum);
                            semi.setProgressColor(ContextCompat.getColor(getContext(), R.color.primary_color));
                        }
                    }

                    double todayGrowth = 0, yesterdayGrowth = 0;
                    double todayTemp = 0, yesterdayTemp = 0;
                    double todayHumid = 0, yesterdayHumid = 0;
                    double todayPh = 0;

                    if (!docs.isEmpty()) {
                        DocumentSnapshot today = docs.get(0);
                        todayGrowth = getAvgGrowth(today);
                        todayTemp = getAvgTemp(today);
                        todayHumid = getAvgHumid(today);
                        todayPh = getAvgPh(today);
                    }
                    if (docs.size() >= 2) {
                        DocumentSnapshot yesterday = docs.get(1);
                        yesterdayGrowth = getAvgGrowth(yesterday);
                        yesterdayTemp = getAvgTemp(yesterday);
                        yesterdayHumid = getAvgHumid(yesterday);
                    }

                    double growthDiffDay = todayGrowth - yesterdayGrowth;
                    
                    if (growthDiffDay >= 0) {
                        int improvedColor = ContextCompat.getColor(getContext(), R.color.recommendation_text);
                        int improvedBg = ContextCompat.getColor(getContext(), R.color.status_improved_bg);
                        
                        tvResultTitle.setText(android.text.Html.fromHtml("Daily Result: <font color='" + String.format("#%06X", (0xFFFFFF & improvedColor)) + "'>IMPROVED</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
                        tvDailyTrendTag.setText(String.format(Locale.getDefault(), "▲ %.1f cm", growthDiffDay));
                        tvDailyTrendTag.setTextColor(improvedColor);
                        tvDailyTrendTag.getParent().requestLayout();
                        ((View)tvDailyTrendTag.getParent()).setBackgroundTintList(android.content.res.ColorStateList.valueOf(improvedBg));
                        
                        ivResultIcon.setImageResource(R.drawable.plant);
                        ivResultIcon.setImageTintList(android.content.res.ColorStateList.valueOf(improvedColor));
                    } else {
                        int declinedColor = ContextCompat.getColor(getContext(), R.color.status_declined_text);
                        int declinedBg = ContextCompat.getColor(getContext(), R.color.status_declined_bg);

                        tvResultTitle.setText(android.text.Html.fromHtml("Daily Result: <font color='" + String.format("#%06X", (0xFFFFFF & declinedColor)) + "'>DECLINED</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
                        tvDailyTrendTag.setText(String.format(Locale.getDefault(), "▼ %.1f cm", Math.abs(growthDiffDay)));
                        tvDailyTrendTag.setTextColor(declinedColor);
                        ((View)tvDailyTrendTag.getParent()).setBackgroundTintList(android.content.res.ColorStateList.valueOf(declinedBg));

                        ivResultIcon.setImageResource(R.drawable.ic_warning);
                        ivResultIcon.setImageTintList(android.content.res.ColorStateList.valueOf(declinedColor));
                    }
                    
                    String formattedDesc = String.format(Locale.getDefault(), "Growth changed by <font color='#00AA5B'><b>%.1f cm</b></font> compared to yesterday.", growthDiffDay);
                    tvResultDescription.setText(android.text.Html.fromHtml(formattedDesc, android.text.Html.FROM_HTML_MODE_LEGACY));

                    // Analysis for WEEKLY stats
                    double week2Sum = 0;
                    int count2 = 0;
                    for (int i = 0; i < Math.min(docs.size(), 7); i++) {
                        week2Sum += getAvgGrowth(docs.get(i));
                        count2++;
                    }
                    double week2AvgGrowth = count2 > 0 ? week2Sum / count2 : 0;

                    double week1Sum = 0;
                    int count1 = 0;
                    for (int i = 7; i < Math.min(docs.size(), 14); i++) {
                        week1Sum += getAvgGrowth(docs.get(i));
                        count1++;
                    }
                    double week1AvgGrowth = count1 > 0 ? week1Sum / count1 : 0;

                    double growthDiffWeek = week2AvgGrowth - week1AvgGrowth;
                    double percentChange = 0;
                    if (week1AvgGrowth > 0) {
                        percentChange = (Math.abs(growthDiffWeek) / week1AvgGrowth) * 100;
                    }

                    tvWeeklyTrendValue.setText(String.format(Locale.getDefault(), "%.1f cm", week2AvgGrowth));
                    tvVsLastWeek.setText(String.format(Locale.getDefault(), "vs last week (%.1f cm)", week1AvgGrowth));
                    
                    if (growthDiffWeek > 0.1) {
                        int improvedColor = ContextCompat.getColor(getContext(), R.color.recommendation_text);
                        int improvedBg = ContextCompat.getColor(getContext(), R.color.status_improved_bg);
                        int deepGreen = ContextCompat.getColor(getContext(), R.color.recommendation_text);

                        tvWeeklyResultTitle.setText(android.text.Html.fromHtml("Weekly Result: <font color='" + String.format("#%06X", (0xFFFFFF & improvedColor)) + "'>IMPROVED</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
                        tvWeeklyResultDescription.setText("Growth is trending upwards this week.");
                        
                        tvWeeklyTrendStatus.setText("Improved");
                        tvWeeklyTrendStatus.setTextColor(improvedColor);
                        
                        tvWeeklyBadgeIcon.setText("▲");
                        tvWeeklyBadgeIcon.setTextColor(improvedColor);
                        tvWeeklyBadgeText.setText("Trending Up");
                        tvWeeklyBadgeText.setTextColor(improvedColor);
                        layoutWeeklyBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(improvedBg));

                        tvWeeklyTrendValue.setTextColor(deepGreen);
                        tvWeeklyTrendDesc.setText("Improving trend");
                        tvWeeklyTrendDetailedDesc.setText("Your plant is growing better than yesterday!");
                        
                        tvWeeklyPercentBadge.setText(String.format(Locale.getDefault(), "▲ %.0f%%", percentChange));
                        tvWeeklyPercentBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(improvedBg));
                        tvWeeklyPercentBadge.setTextColor(deepGreen);
                        
                        ivWeeklyIcon.setImageResource(R.drawable.ic_trending_up);
                        ivWeeklyIcon.setImageTintList(android.content.res.ColorStateList.valueOf(deepGreen));
                        ivTrendArrow.setImageResource(R.drawable.ic_trending_up);
                        ivTrendArrow.setImageTintList(android.content.res.ColorStateList.valueOf(deepGreen));
                        ivTrendArrow.setVisibility(View.VISIBLE);
                        layoutWeeklyBottomPanel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(improvedBg));

                    } else if (growthDiffWeek < -0.1) {
                        int declinedColor = ContextCompat.getColor(getContext(), R.color.status_declined_text);
                        int declinedBg = ContextCompat.getColor(getContext(), R.color.status_declined_bg);

                        tvWeeklyResultTitle.setText(android.text.Html.fromHtml("Weekly Result: <font color='" + String.format("#%06X", (0xFFFFFF & declinedColor)) + "'>DECLINED</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
                        tvWeeklyResultDescription.setText("Plant growth has slowed down this week.");
                        
                        tvWeeklyTrendStatus.setText("Declined");
                        tvWeeklyTrendStatus.setTextColor(declinedColor);

                        tvWeeklyBadgeIcon.setText("▼");
                        tvWeeklyBadgeIcon.setTextColor(declinedColor);
                        tvWeeklyBadgeText.setText("Trending Down");
                        tvWeeklyBadgeText.setTextColor(declinedColor);
                        layoutWeeklyBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(declinedBg));

                        tvWeeklyTrendValue.setTextColor(declinedColor);
                        tvWeeklyTrendDesc.setText("Slowing growth");
                        tvWeeklyTrendDetailedDesc.setText("Growth rate has decreased recently.");

                        tvWeeklyPercentBadge.setText(String.format(Locale.getDefault(), "▼ %.0f%%", percentChange));
                        tvWeeklyPercentBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(declinedBg));
                        tvWeeklyPercentBadge.setTextColor(declinedColor);

                        ivWeeklyIcon.setImageResource(R.drawable.ic_warning);
                        ivWeeklyIcon.setImageTintList(android.content.res.ColorStateList.valueOf(declinedColor));
                        ivTrendArrow.setImageResource(R.drawable.ic_warning);
                        ivTrendArrow.setImageTintList(android.content.res.ColorStateList.valueOf(declinedColor));
                        ivTrendArrow.setVisibility(View.VISIBLE);
                        layoutWeeklyBottomPanel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(declinedBg));

                    } else {
                        int stableColor = ContextCompat.getColor(getContext(), R.color.status_stable_text);
                        int stableBg = ContextCompat.getColor(getContext(), R.color.status_stable_bg);

                        tvWeeklyResultTitle.setText(android.text.Html.fromHtml("Weekly Result: <font color='" + String.format("#%06X", (0xFFFFFF & stableColor)) + "'>STABLE</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
                        tvWeeklyResultDescription.setText("Growth rate is consistent with last week.");
                        
                        tvWeeklyTrendStatus.setText("Stable");
                        tvWeeklyTrendStatus.setTextColor(stableColor);

                        tvWeeklyBadgeIcon.setText("—");
                        tvWeeklyBadgeIcon.setTextColor(stableColor);
                        tvWeeklyBadgeText.setText("No Change");
                        tvWeeklyBadgeText.setTextColor(stableColor);
                        layoutWeeklyBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(stableBg));

                        tvWeeklyTrendValue.setTextColor(stableColor);
                        tvWeeklyTrendDesc.setText("Stable trend");
                        tvWeeklyTrendDetailedDesc.setText("Maintain current conditions.");

                        tvWeeklyPercentBadge.setText("— 0%");
                        tvWeeklyPercentBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(stableBg));
                        tvWeeklyPercentBadge.setTextColor(stableColor);

                        ivWeeklyIcon.setImageResource(R.drawable.ic_trending_up); // maybe find a better icon or rotate
                        ivWeeklyIcon.setImageTintList(android.content.res.ColorStateList.valueOf(stableColor));
                        ivTrendArrow.setVisibility(View.GONE);
                        layoutWeeklyBottomPanel.setBackgroundTintList(android.content.res.ColorStateList.valueOf(stableBg));
                    }

                    // Populate Causes and Actions
                    layoutCauses.removeAllViews();
                    layoutActions.removeAllViews();
                    int actionCount = 0;

                    // pH Check
                    if (todayPh < 5.5 || todayPh > 6.5) {
                        addCauseRow("pH Imbalance", String.format(Locale.getDefault(), "%.1f (optimal range 5.5 - 6.5)", todayPh));
                        addActionRow(++actionCount, "Adjust pH", todayPh < 5.5 ? "Increase pH to maintain between 5.5 - 6.5." : "Decrease pH to maintain between 5.5 - 6.5.");
                    }

                    // Humidity Check
                    double humidDiff = todayHumid - yesterdayHumid;
                    if (Math.abs(humidDiff) > 5 || todayHumid < 50) {
                        addCauseRow(humidDiff > 0 ? "Humidity Increased" : "Humidity Decreased", String.format(Locale.getDefault(), "%+.0f%% change from yesterday", humidDiff));
                        if (todayHumid < 50) addActionRow(++actionCount, "Increase Humidity", "Maintain humidity around 60-70% for optimal growth.");
                    }

                    // Temperature Check
                    double tempDiff = todayTemp - yesterdayTemp;
                    if (Math.abs(tempDiff) > 2) {
                        addCauseRow(tempDiff > 0 ? "Temperature Increased" : "Temperature Decreased", String.format(Locale.getDefault(), "%+.1f °C change", tempDiff));
                        addActionRow(++actionCount, "Stabilize Temperature", "Avoid fluctuations; keep between 18-24°C if possible.");
                    }

                    // Default action
                    addActionRow(++actionCount, "Monitor Consistently", "Keep monitoring daily for better results.");

                    cardCauses.setVisibility(layoutCauses.getChildCount() > 0 ? View.VISIBLE : View.GONE);
                    cardActions.setVisibility(View.VISIBLE);

                    String recTitle, recMsg;
                    if (growthDiffDay > 0 && growthDiffWeek > 0) {
                        recTitle = "Excellent growth.";
                        recMsg = "Maintain current setup.";
                    } else if (growthDiffDay > 0 && growthDiffWeek < 0) {
                        recTitle = "Improving slowly.";
                        recMsg = "Overall growth trend is still below average.";
                    } else if (growthDiffDay < 0 && growthDiffWeek < 0) {
                        recTitle = "Critical decline.";
                        recMsg = "Check nutrient levels and light immediately.";
                    } else if (growthDiffDay < 0 && growthDiffWeek > 0) {
                        recTitle = "Temporary decline.";
                        recMsg = "Monitor before making major changes.";
                    } else {
                        recTitle = "Growth plateau.";
                        recMsg = "Adjust nutrients or pH slightly for better results.";
                    }
                    tvRecommendationTitle.setText(recTitle);
                    tvRecommendationMessage.setText(recMsg);

                })
                .addOnFailureListener(e -> Log.e("GrowthFragment", "Error fetching data", e));
    }

    private void addCauseRow(String label, String value) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));
        
        TextView tvLabel = new TextView(getContext());
        tvLabel.setText(label);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.cause_label));
        tvLabel.setTextSize(13);
        
        TextView tvValue = new TextView(getContext());
        tvValue.setText(value);
        tvValue.setTextColor(ContextCompat.getColor(getContext(), R.color.cause_value));
        tvValue.setTextSize(13);
        tvValue.setGravity(Gravity.END);
        
        row.addView(tvLabel);
        row.addView(tvValue);
        layoutCauses.addView(row);
    }

    private void addActionRow(int index, String title, String desc) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dpToPx(10), 0, dpToPx(10));
        
        TextView tvNum = new TextView(getContext());
        tvNum.setText(index + ".");
        tvNum.setPadding(0, 0, dpToPx(12), 0);
        tvNum.setTypeface(null, Typeface.BOLD);
        tvNum.setTextColor(ContextCompat.getColor(getContext(), R.color.app_text));
        
        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(getContext(), R.color.app_text));
        tvTitle.setTextSize(14);
        
        TextView tvDesc = new TextView(getContext());
        tvDesc.setText(desc);
        tvDesc.setTextSize(12);
        tvDesc.setTextColor(ContextCompat.getColor(getContext(), R.color.cause_label));
        
        textCol.addView(tvTitle);
        textCol.addView(tvDesc);
        
        row.addView(tvNum);
        row.addView(textCol);
        layoutActions.addView(row);
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private double getAvgGrowth(DocumentSnapshot doc) {
        GrowthRecord record = doc.toObject(GrowthRecord.class);
        if (record != null && record.getDailyAverage() != null) {
            return record.getDailyAverage().getAvgGrowthSize();
        }
        return 0;
    }
    private double getAvgTemp(DocumentSnapshot doc) {
        GrowthRecord record = doc.toObject(GrowthRecord.class);
        if (record != null && record.getDailyAverage() != null) {
            return record.getDailyAverage().getAvgTemperature();
        }
        return 25;
    }
    private double getAvgHumid(DocumentSnapshot doc) {
        GrowthRecord record = doc.toObject(GrowthRecord.class);
        if (record != null && record.getDailyAverage() != null) {
            return record.getDailyAverage().getAvgHumidity();
        }
        return 60;
    }
    private double getAvgPh(DocumentSnapshot doc) {
        GrowthRecord record = doc.toObject(GrowthRecord.class);
        if (record != null && record.getDailyAverage() != null) {
            return record.getDailyAverage().getAvgPh();
        }
        return 6.0;
    }

    private void loadChartDataFromFirestore() {
        GreenhouseRepository.GreenhouseData data = GreenhouseRepository.getInstance().getCurrentGreenhouse();
        if (data == null) {
            Log.w("GrowthFragment", "loadChartDataFromFirestore: Greenhouse data is null, skipping update");
            return;
        }

        String currentBatch = data.getCurrentBatch();
        String greenhouseId = data.getGreenhouseID();
        
        Log.d("GrowthFragment", "analyzeGrowthData: Querying Records for Greenhouse=" + greenhouseId + " Batch=" + currentBatch);

        db.collection("PlantGrowthData")
                .document(greenhouseId)
                .collection("Batches")
                .document(currentBatch)
                .collection("Records")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded() || getContext() == null) return;
                    List<DocumentSnapshot> docs = new ArrayList<>(queryDocumentSnapshots.getDocuments());
                    Collections.sort(docs, (d1, d2) -> d2.getId().compareTo(d1.getId()));
                    if (docs.size() > 7) {
                        docs = docs.subList(0, 7);
                    }
                    Collections.reverse(docs);
                    int count = docs.size();
                    loadedValues = new float[7];
                    String[] dynamicLabels = new String[7];
                    if (!docs.isEmpty()) {
                        for (int i = 0; i < 7; i++) {
                            if (i < count) {
                                DocumentSnapshot doc = docs.get(i);
                                loadedValues[i] = (float) getAvgGrowth(doc);
                                
                                String dayStr = getDayString(doc);
                                int dayNumber = parseDayNumber(dayStr);
                                if (dayNumber > 0) {
                                    dynamicLabels[i] = String.valueOf(dayNumber);
                                    Log.d("GrowthChart", "Record=" + doc.getId() + " | day=" + dayStr + " | avgGrowthSize=" + loadedValues[i]);
                                } else {
                                    dynamicLabels[i] = "-";
                                    Log.d("GrowthChart", "Record=" + doc.getId() + " | day missing | avgGrowthSize=" + loadedValues[i]);
                                }
                            } else {
                                loadedValues[i] = 0f;
                                dynamicLabels[i] = "-";
                            }
                        }
                        Log.d("GrowthChart", "entriesCount = " + count);
                    } else {
                        for (int i = 0; i < 7; i++) {
                            loadedValues[i] = 0f;
                            dynamicLabels[i] = "-";
                        }
                    }
                    dayLabels = dynamicLabels;
                    chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dayLabels));
                    highlightIndex = Math.max(0, count - 1);
                    chart.setAllValues(loadedValues);

                    // Fixed Y-axis scale 0-100 cm²
                    chart.getAxisLeft().setAxisMinimum(0f);
                    chart.getAxisLeft().setAxisMaximum(100f);

                    if (getContext() != null) {
                        updateChart(loadedValues);
                    }
                })
                .addOnFailureListener(e -> Log.e("GrowthFragment", "Error loading chart data", e));
    }

    private void updateChart(float[] lineValues) {
        buildAndSetData(lineValues, highlightIndex);
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int tappedIndex = (int) e.getX();
                if (tappedIndex < 0 || tappedIndex >= lineValues.length) return;
                highlightIndex = tappedIndex;
                buildAndSetData(lineValues, tappedIndex);
                chart.setTooltipIndex(tappedIndex, lineValues[tappedIndex]);
                chart.invalidate();
            }
            @Override
            public void onNothingSelected() {}
        });
        chart.setTooltipIndex(highlightIndex, lineValues[highlightIndex]);
        chart.invalidate();
    }

    private void buildAndSetData(float[] lineValues, int activeIndex) {
        if (getContext() == null) return;
        List<BarEntry> barEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            float val = 0f;
            if (i < dayLabels.length && !"-".equals(dayLabels[i])) {
                val = lineValues[i];
            }
            barEntries.add(new BarEntry(i, val));
        }
        BarDataSet barDataSet = new BarDataSet(barEntries, "");
        int[] barColors = new int[7];
        int activeColor = ContextCompat.getColor(getContext(), R.color.status_improved_icon);
        for (int i = 0; i < 7; i++) {
            barColors[i] = (i == activeIndex) ? 
                    Color.argb(80, Color.red(activeColor), Color.green(activeColor), Color.blue(activeColor)) : 
                    Color.argb(60, 180, 190, 180);
        }
        barDataSet.setColors(barColors);
        barDataSet.setDrawValues(false);
        barDataSet.setHighlightEnabled(false);
        BarData barData = new BarData(barDataSet);
        barData.setBarWidth(0.72f);

        List<Entry> lineEntries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            // Only add entries if there is actual data for that day (label is not "-")
            if (i < dayLabels.length && !"-".equals(dayLabels[i])) {
                lineEntries.add(new Entry(i, lineValues[i]));
            }
        }

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Growth (cm²)");
        lineDataSet.setColor(activeColor);
        lineDataSet.setLineWidth(2.5f);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineDataSet.setDrawValues(false);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setCircleRadius(5f);
        int[] circleColors = new int[7];
        for (int i = 0; i < 7; i++) {
            circleColors[i] = (i == activeIndex) ? activeColor : Color.TRANSPARENT;
        }
        lineDataSet.setCircleColors(circleColors);
        lineDataSet.setCircleHoleColor(activeColor);
        LineData lineData = new LineData(lineDataSet);
        CombinedData combinedData = new CombinedData();
        combinedData.setData(barData);
        combinedData.setData(lineData);
        chart.setData(combinedData);
        chart.setDrawOrder(new CombinedChart.DrawOrder[]{CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.LINE});
    }

    private void setupChartAppearance() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setScaleEnabled(false);
        chart.setXAxisRenderer(new HighlightXAxisRenderer(chart.getViewPortHandler(), chart.getXAxis(), chart.getTransformer(YAxis.AxisDependency.LEFT)));
        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(6.5f);
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#EEEEEE"));
        leftAxis.setDrawAxisLine(false);
        leftAxis.setDrawLabels(true);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setLabelCount(11, true);
        leftAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.text_color));
        leftAxis.setTextSize(10f);
        leftAxis.setXOffset(8f); // Padding for labels
        chart.getAxisRight().setEnabled(false);
    }

    class HighlightXAxisRenderer extends com.github.mikephil.charting.renderer.XAxisRenderer {
        public HighlightXAxisRenderer(com.github.mikephil.charting.utils.ViewPortHandler viewPortHandler, XAxis xAxis, com.github.mikephil.charting.utils.Transformer transformer) {
            super(viewPortHandler, xAxis, transformer);
        }
        @Override
        protected void drawLabel(Canvas c, String formattedLabel, float x, float y, com.github.mikephil.charting.utils.MPPointF anchor, float angleDegrees) {
            if (highlightIndex < dayLabels.length && formattedLabel.equalsIgnoreCase(dayLabels[highlightIndex])) {
                mAxisLabelPaint.setColor(ContextCompat.getColor(getContext(), R.color.primary_color));
                mAxisLabelPaint.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                mAxisLabelPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_color));
                mAxisLabelPaint.setTypeface(Typeface.DEFAULT);
            }
            super.drawLabel(c, formattedLabel, x, y, anchor, anchor != null ? angleDegrees : 0);
        }
    }

    private int parseDayNumber(String dayValue) {
        if (dayValue == null || dayValue.trim().isEmpty()) {
            return 0;
        }

        try {
            String number = dayValue.replaceAll("[^0-9]", "");
            return number.isEmpty() ? 0 : Integer.parseInt(number);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getDayString(DocumentSnapshot doc) {
        // Try mapping to GrowthRecord first
        GrowthRecord record = doc.toObject(GrowthRecord.class);
        if (record != null && record.getDailyAverage() != null) {
            String day = record.getDailyAverage().getDay();
            if (day != null && !day.isEmpty()) return day;
        }

        // Fallback to manual map access if toObject failed or field is missing in DailyAverage
        try {
            Map<String, Object> daily = (Map<String, Object>) doc.get("DailyBestCondition");
            if (daily != null && daily.containsKey("day")) {
                Object dayVal = daily.get("day");
                if (dayVal != null) return dayVal.toString();
            }
        } catch (Exception e) {
            Log.e("GrowthFragment", "Error manually parsing day field from " + doc.getId(), e);
        }
        return null;
    }
}
