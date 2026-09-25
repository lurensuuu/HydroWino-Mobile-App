package com.example.hydrowino;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class TreatmentActivity extends AppCompatActivity {

    private ImageView imgCaptured;
    private ImageButton btnBack, btnClose;
    private TextView tvTitle;
    private LinearLayout preventionContainer, treatmentContainer;
    private Button btnScanAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treatment);

        imgCaptured = findViewById(R.id.imgCaptured);
        btnBack = findViewById(R.id.btnBack);
        btnClose = findViewById(R.id.btnClose);
        tvTitle = findViewById(R.id.tvTitle);
        preventionContainer = findViewById(R.id.preventionContainer);
        treatmentContainer = findViewById(R.id.treatmentContainer);
        btnScanAgain = findViewById(R.id.btnScanAgain);

        String imageUrl = getIntent().getStringExtra("image_url");
        String prediction = getIntent().getStringExtra("prediction");

        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).into(imgCaptured);
        }

        if (prediction != null) {
            tvTitle.setText(prediction);
            loadTreatmentData(prediction);
        }

        btnBack.setOnClickListener(v -> finish());
        btnClose.setOnClickListener(v -> finish());
        btnScanAgain.setOnClickListener(v -> finish());
    }

    private void loadTreatmentData(String prediction) {
        preventionContainer.removeAllViews();
        treatmentContainer.removeAllViews();

        switch (prediction) {
            case "Nitrogen Deficient":
                addPoint(preventionContainer, "Maintain proper pH (5.5-6.5):", "Incorrect pH can block nitrogen uptake even if it's present in the water.", true);
                addPoint(preventionContainer, "Regular Monitoring:", "Check EC (Electrical Conductivity) levels weekly to ensure nutrients are balanced.", true);
                
                addPoint(treatmentContainer, "Apply Nitrogen-Rich Solution:", "Increase nitrogen levels using calcium nitrate or a complete hydroponic nutrient mix.", false);
                addPoint(treatmentContainer, "Foliar Feeding:", "Spray a diluted nitrogen solution directly on leaves for faster absorption.", false);
                break;

            case "Phosphorus Deficient":
                addPoint(preventionContainer, "Ensure Proper Temperature:", "Low root zone temperatures (below 15°C) can inhibit phosphorus uptake.", true);
                addPoint(preventionContainer, "Optimal pH Levels:", "Keep pH between 5.8 and 6.2 for best phosphorus availability.", true);
                
                addPoint(treatmentContainer, "Adjust Nutrient Balance:", "Add mono-potassium phosphate to the reservoir to boost phosphorus levels.", false);
                addPoint(treatmentContainer, "Reservoir Flush:", "If nutrient lockout is suspected, flush the system with pH-balanced water.", false);
                break;

            case "Potassium Deficient":
                addPoint(preventionContainer, "Balanced Nutrients:", "Ensure the N-P-K ratio is appropriate for the current growth stage (more K during flowering/fruiting).", true);
                addPoint(preventionContainer, "Water Quality:", "Use filtered or RO water to prevent mineral competition.", true);
                
                addPoint(treatmentContainer, "Increase Potassium Levels:", "Use potassium sulfate or potassium silicate to correct the deficiency.", false);
                addPoint(treatmentContainer, "Check Oxygenation:", "Poor root oxygenation can hinder K uptake; ensure air stones are working.", false);
                break;

            case "Healthy":
                addPoint(preventionContainer, "Steady Routine:", "Keep following your successful maintenance schedule.", true);
                addPoint(treatmentContainer, "Observation:", "Continue to monitor for any subtle changes in leaf color or growth rate.", false);
                break;
                
            default:
                addPoint(preventionContainer, "System Check:", "Check all sensors and pumps for proper operation.", true);
                addPoint(treatmentContainer, "Consult Expert:", "If symptoms persist, seek advice from a local hydroponics specialist.", false);
                break;
        }
    }

    private void addPoint(LinearLayout container, String boldText, String normalText, boolean isPrevention) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        itemLayout.setPadding(0, 8, 0, 8);

        ImageView icon = new ImageView(this);
        int iconSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.setMargins(0, 10, 20, 0);
        icon.setLayoutParams(iconParams);
        
        if (isPrevention) {
            icon.setImageResource(R.drawable.plant); 
            icon.setColorFilter(Color.parseColor("#4CAF50"));
        } else {
            icon.setImageResource(R.drawable.drop1); 
            icon.setColorFilter(Color.parseColor("#FF5252"));
        }
        
        itemLayout.addView(icon);

        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        
        TextView title = new TextView(this);
        title.setText(boldText);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        textLayout.addView(title);

        TextView detail = new TextView(this);
        detail.setText(normalText);
        detail.setTextColor(Color.parseColor("#666666"));
        detail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textLayout.addView(detail);

        itemLayout.addView(textLayout);
        container.addView(itemLayout);
    }
}