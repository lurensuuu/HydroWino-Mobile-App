package com.example.hydrowino;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BatchDetailsActivity extends AppCompatActivity {

    private PlantBatch batch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_batch_details);

        batch = (PlantBatch) getIntent().getSerializableExtra("batch");
        if (batch == null) {
            finish();
            return;
        }

        setupHeader();
        setupSummaryCard();
        setupCharts();
    }

    private void setupHeader() {
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
        TextView tvTitle = findViewById(R.id.tvTitle);
        tvTitle.setText(batch.getName() + " Details");
    }

    private void setupSummaryCard() {
        ImageView ivPlant = findViewById(R.id.ivPlant);
        TextView tvBatchName = findViewById(R.id.tvBatchName);
        TextView tvStatus = findViewById(R.id.tvStatus);
        TextView tvPlantType = findViewById(R.id.tvPlantType);
        TextView tvDateRange = findViewById(R.id.tvDateRange);
        
        TextView tvAvgGrowth = findViewById(R.id.tvAvgGrowth);
        TextView tvWaterUsed = findViewById(R.id.tvWaterUsed);
        TextView tvAvgTemp = findViewById(R.id.tvAvgTemp);
        TextView tvAvgPh = findViewById(R.id.tvAvgPh);
        TextView tvAvgHumidity = findViewById(R.id.tvAvgHumidity);
        TextView tvAvgTds = findViewById(R.id.tvAvgTds);
        TextView tvWaterTemp = findViewById(R.id.tvWaterTemp);

        tvBatchName.setText(batch.getName());
        tvStatus.setText(batch.getStatus());
        tvPlantType.setText(batch.getType());
        tvDateRange.setText(batch.getDateRange() + " (" + batch.getDuration() + ")");

        tvAvgGrowth.setText(batch.getAvgGrowth() != null ? batch.getAvgGrowth() : "25.3 cm");
        tvWaterUsed.setText(batch.getWaterUsed() != null ? batch.getWaterUsed() : "38 L");
        
        tvAvgTemp.setText(String.format(Locale.getDefault(), "%.1f °C", batch.getAvgTemp()));
        tvAvgPh.setText(String.format(Locale.getDefault(), "%.1f", batch.getAvgPh()));
        tvAvgHumidity.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getAvgHumidity()));
        tvAvgTds.setText(String.format(Locale.getDefault(), "%.0f ppm", batch.getAvgTds() != 0 ? batch.getAvgTds() : 850));
        tvWaterTemp.setText(String.format(Locale.getDefault(), "%.1f °C", batch.getAvgWaterTemp() != 0 ? batch.getAvgWaterTemp() : 24.5));

        Glide.with(this)
                .load(batch.getImageUrl())
                .placeholder(R.drawable.lettuce)
                .into(ivPlant);
    }

    private void setupCharts() {
        LineChart tempChart = findViewById(R.id.tempChart);
        LineChart phChart = findViewById(R.id.phChart);
        LineChart humidityChart = findViewById(R.id.humidityChart);
        LineChart growthChart = findViewById(R.id.growthChart);

        String[] labels = {"May 14", "May 21", "May 28", "Jun 4", "Jun 11"};

        setupLineChart(tempChart, getDummyData(24, 28, 15), "#E67E22", labels);
        setupLineChart(phChart, getDummyData(5.8f, 6.4f, 15), "#3498DB", labels);
        setupLineChart(humidityChart, getDummyData(60, 80, 15), "#27AE60", labels);
        setupLineChart(growthChart, getGrowthDummyData(15), "#9B59B6", labels);
    }

    private List<Entry> getDummyData(float min, float max, int count) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            float val = min + (float)(Math.random() * (max - min));
            entries.add(new Entry(i, val));
        }
        return entries;
    }

    private List<Entry> getGrowthDummyData(int count) {
        List<Entry> entries = new ArrayList<>();
        float val = 0;
        for (int i = 0; i < count; i++) {
            val += (float)(Math.random() * 3);
            entries.add(new Entry(i, val));
        }
        return entries;
    }

    private void setupLineChart(LineChart chart, List<Entry> entries, String colorStr, String[] labels) {
        LineDataSet dataSet = new LineDataSet(entries, "");
        int color = Color.parseColor(colorStr);
        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);

        int textColor = ContextCompat.getColor(this, R.color.text_color);
        int gridColor = ContextCompat.getColor(this, R.color.divider_color);
        
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(3f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(textColor);
        xAxis.setTextSize(9f);

        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisLeft().setGridColor(gridColor);
        chart.getAxisLeft().setTextColor(textColor);
        chart.getAxisLeft().setTextSize(9f);

        chart.invalidate();
    }
}
