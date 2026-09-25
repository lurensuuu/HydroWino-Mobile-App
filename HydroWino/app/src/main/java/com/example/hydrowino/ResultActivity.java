package com.example.hydrowino;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ResultActivity extends AppCompatActivity {

    private ImageView imgCaptured;
    private ImageButton btnBack;
    private TextView tvTitle, tvSubtitle, tvDetectedCount, tvSummaryText, tvDescription;
    private View statusBanner;
    private GridLayout glDetections;
    private LinearLayout llTreatments;
    private Button btnScanAnother;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        imgCaptured = findViewById(R.id.imgCaptured);
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvDetectedCount = findViewById(R.id.tvDetectedCount);
        glDetections = findViewById(R.id.glDetections);
        tvSummaryText = findViewById(R.id.tvSummaryText);
        tvDescription = findViewById(R.id.tvDescription);
        llTreatments = findViewById(R.id.llTreatments);
        btnScanAnother = findViewById(R.id.btnScanAnother);
        statusBanner = findViewById(R.id.statusBanner);

        String imageUrl = getIntent().getStringExtra("image_url");
        String annotatedImagePath = getIntent().getStringExtra("annotated_image_path");
        String prediction = getIntent().getStringExtra("prediction"); // For "Not Lettuce" case
        
        ArrayList<PlantDetectionResult> detections = getIntent().getParcelableArrayListExtra("detections");
        String overallResult = getIntent().getStringExtra("overall_result");
        
        @SuppressWarnings("unchecked")
        HashMap<String, Integer> summaryMap = (HashMap<String, Integer>) getIntent().getSerializableExtra("summary");
        ArrayList<String> uniqueDeficiencies = getIntent().getStringArrayListExtra("unique_deficiencies");

        if (annotatedImagePath != null) {
            Glide.with(this).load(new File(annotatedImagePath)).into(imgCaptured);
        } else if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imgCaptured);
        }

        if ("Not Lettuce".equals(prediction)) {
            showNoLettuceFound();
        } else if (detections != null && !detections.isEmpty()) {
            showAnalysisResults(detections, overallResult, summaryMap, uniqueDeficiencies);
        }

        btnBack.setOnClickListener(v -> finish());
        if (btnScanAnother != null) {
            btnScanAnother.setOnClickListener(v -> finish());
        }
    }

    private void showNoLettuceFound() {
        tvTitle.setText("No Lettuce Found");
        tvSubtitle.setText("We couldn't detect a supported lettuce plant in your image.");
        tvDescription.setText("The scanned image does not appear to contain a supported lettuce plant. Please capture a clearer lettuce image for nutrient analysis.");
        
        if (statusBanner != null) {
            TextView subtitleView = statusBanner.findViewById(R.id.tvBannerSubtitle);
            if (subtitleView != null) subtitleView.setText("Detection failed.");
            ImageView iconView = statusBanner.findViewById(R.id.ivBannerStatusIcon);
            if (iconView != null) {
                iconView.setImageResource(R.drawable.forbidden24);
                iconView.setImageTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#C62828")));
            }
        }
        llTreatments.removeAllViews();
        llTreatments.addView(createTreatmentView("Scanning Tips", 
                "Treatment: Use an actual lettuce plant when scanning.\n" +
                "Recommendation: Place the lettuce clearly inside the frame, use proper lighting, and avoid blurry images.", 
                false));
        
        glDetections.setVisibility(View.GONE);
        btnScanAnother.setText("Scan Again");
    }

    private void showAnalysisResults(ArrayList<PlantDetectionResult> detections,
                                     String overallResult,
                                     HashMap<String, Integer> summaryMap,
                                     ArrayList<String> uniqueDeficiencies) {
        
        tvTitle.setText(overallResult);
        
        boolean isAllHealthy = "Healthy plants detected.".equals(overallResult);

        if (statusBanner != null) {
            if (isAllHealthy) {
                TextView subtitleView = statusBanner.findViewById(R.id.tvBannerSubtitle);
                if (subtitleView != null) subtitleView.setText("We analyzed your image successfully.");
            } else {
                TextView subtitleView = statusBanner.findViewById(R.id.tvBannerSubtitle);
                if (subtitleView != null) subtitleView.setText("Issues detected.");
            }
        }

        if (isAllHealthy) {
            tvSubtitle.setText("Great! Your plants appear to be healthy and well-nourished.");
            tvDescription.setText("Your plants look healthy! Keep maintaining the current nutrient levels, pH, and environmental conditions.");
        } else {
            tvSubtitle.setText("Some detected plants are healthy, while others need attention.");
            tvDescription.setText("Some detected plants appear healthy, while one or more plants show signs of nutrient deficiency. Review each plant result and follow the recommendations for the affected plants.");
        }
        
        tvDetectedCount.setText(String.format(Locale.US, "Detected Plants: %d", detections.size()));
        
        // Populate Detections Grid
        glDetections.removeAllViews();
        int total = detections.size();
        for (PlantDetectionResult res : detections) {
            glDetections.addView(createDetectionCard(res, total));
        }
        
        // Populate Summary
        StringBuilder summaryStr = new StringBuilder();
        if (summaryMap != null) {
            for (Map.Entry<String, Integer> entry : summaryMap.entrySet()) {
                summaryStr.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        tvSummaryText.setText(summaryStr.toString().trim());
        
        // Populate Treatments
        llTreatments.removeAllViews();
        if ("Healthy plants detected.".equals(overallResult)) {
            llTreatments.addView(createTreatmentView("Maintenance", 
                    "Treatment: No treatment required.\nRecommendation: Continue maintaining proper pH, TDS, water, and environmental conditions.", 
                    false));
        } else {
            if (uniqueDeficiencies != null) {
                for (String def : uniqueDeficiencies) {
                    llTreatments.addView(createTreatmentView(def, getTreatmentForDeficiency(def), true));
                }
            }
        }
    }

    private View createDetectionCard(PlantDetectionResult res, int totalCount) {
        CardView card = new CardView(this);
        
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        
        // If there's only one detection, make it span both columns to avoid layout issues
        int columnSpan = (totalCount == 1) ? 2 : 1;
        
        GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), // row spec
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, (float) columnSpan) // column spec
        );
        
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.setMargins(margin, margin, margin, margin);
        params.setGravity(android.view.Gravity.FILL_HORIZONTAL);
        
        card.setLayoutParams(params);
        card.setRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.TRANSPARENT);
        card.setPreventCornerOverlap(false);
        card.setUseCompatPadding(false);
        
        boolean isHealthy = "Healthy".equalsIgnoreCase(res.getClassName());
        int bgResId = isHealthy ? R.drawable.plant_card_healthy_bg : R.drawable.plant_card_deficient_bg;
        
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setBackgroundResource(bgResId);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics());
        inner.setPadding(padding, padding, padding, padding);
        
        TextView tvPlant = new TextView(this);
        tvPlant.setText(String.format(Locale.US, "Plant %d", res.getPlantNumber()));
        tvPlant.setTypeface(null, Typeface.BOLD);
        tvPlant.setTextColor(Color.BLACK);
        tvPlant.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        
        TextView tvClass = new TextView(this);
        tvClass.setText(res.getClassName());
        tvClass.setTextColor(isHealthy ? Color.parseColor("#2E7D32") : Color.parseColor("#E64A19"));
        tvClass.setTypeface(null, Typeface.BOLD);
        tvClass.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvClass.setPadding(0, 4, 0, 4);
        
        TextView tvConf = new TextView(this);
        tvConf.setText(String.format(Locale.US, "Confidence: %.2f%%", res.getConfidence()));
        tvConf.setTextColor(Color.GRAY);
        tvConf.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        
        inner.addView(tvPlant);
        inner.addView(tvClass);
        inner.addView(tvConf);
        
        card.addView(inner);
        return card;
    }

    private View createTreatmentView(String title, String content, boolean isDeficiency) {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        card.setCardElevation(0);
        card.setCardBackgroundColor(Color.TRANSPARENT);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setBackgroundResource(R.drawable.treatment_card_bg);
        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        inner.setPadding(padding, padding, padding, padding);

        TextView tvTitleView = new TextView(this);
        tvTitleView.setText(title);
        tvTitleView.setTypeface(null, Typeface.BOLD);
        tvTitleView.setTextColor(isDeficiency ? Color.parseColor("#E64A19") : Color.BLACK);
        tvTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitleView.setPadding(0, 0, 0, 8);

        TextView tvContentView = new TextView(this);
        
        // Bold "Treatment:" and "Recommendation:"
        SpannableString spannable = new SpannableString(content);
        int tIdx = content.indexOf("Treatment:");
        if (tIdx >= 0) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), tIdx, tIdx + 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        int rIdx = content.indexOf("Recommendation:");
        if (rIdx >= 0) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), rIdx, rIdx + 15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        tvContentView.setText(spannable);
        tvContentView.setTextColor(Color.parseColor("#333333"));
        tvContentView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tvContentView.setLineSpacing(4, 1);

        inner.addView(tvTitleView);
        inner.addView(tvContentView);
        card.addView(inner);
        return card;
    }

    private String getTreatmentForDeficiency(String deficiency) {
        switch (deficiency) {
            case "Nitrogen Deficiency":
                return "Treatment: Check and adjust the nutrient solution to provide adequate nitrogen.\nRecommendation: Monitor TDS, pH, and older leaves for continued yellowing.";
            case "Phosphorus Deficiency":
                return "Treatment: Check the nutrient solution and ensure adequate phosphorus is available.\nRecommendation: Maintain proper pH and monitor plant growth and leaf coloration.";
            case "Potassium Deficiency":
                return "Treatment: Check and correct the potassium level in the nutrient solution.\nRecommendation: Observe older leaf edges for yellowing, browning, or curling.";
            case "Calcium Deficiency":
                return "Treatment: Check the nutrient solution and calcium availability.\nRecommendation: Monitor new leaves for distorted growth or damaged leaf tips.";
            case "Magnesium Deficiency":
                return "Treatment: Check the nutrient solution and magnesium availability.\nRecommendation: Monitor older leaves for yellowing between the leaf veins.";
            default:
                return "Treatment: Check nutrient solution balance.\nRecommendation: Monitor pH levels.";
        }
    }
}
