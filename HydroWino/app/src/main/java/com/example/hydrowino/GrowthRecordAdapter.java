package com.example.hydrowino;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class GrowthRecordAdapter extends RecyclerView.Adapter<GrowthRecordAdapter.ViewHolder> {

    private List<Object> items;
    private OnItemClickListener listener;
    private String greenhouseId;
    private String batchName;

    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    public GrowthRecordAdapter(List<Object> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setMetadata(String greenhouseId, String batchName) {
        this.greenhouseId = greenhouseId;
        this.batchName = batchName;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_growth_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object item = items.get(position);

        if (item instanceof WeeklyRecord) {
            WeeklyRecord weekly = (WeeklyRecord) item;
            holder.tvDay.setText(holder.itemView.getContext().getString(R.string.week) + " " + weekly.getWeekNumber());
            holder.tvDateTime.setText(weekly.getDateRange());
            holder.tvTemp.setText(String.format(Locale.getDefault(), "%.1f °C", weekly.getAvgTemperature()));
            holder.tvHumidity.setText(String.format(Locale.getDefault(), "%.0f%%", weekly.getAvgHumidity()));
            holder.tvPh.setText(String.format(Locale.getDefault(), "%.1f", weekly.getAvgPh()));
            holder.tvGrowthSize.setText(String.format(Locale.getDefault(), "%.1f cm", weekly.getAvgGrowthSize()));
            
            holder.rlFooter.setVisibility(View.VISIBLE);
            holder.sparklineChart.setVisibility(View.VISIBLE);
            holder.btnViewDetails.setVisibility(View.VISIBLE);
            holder.pbGrowth.setVisibility(View.GONE);
            
            // Setup sparkline with dummy data or real data if available in WeeklyRecord
            setupSparkline(holder.sparklineChart, getSparklineDataForWeekly(weekly));

            // Load the last photo of the week
            if (weekly.getDailyRecords() != null && !weekly.getDailyRecords().isEmpty()) {
                // weekly.getDailyRecords() is sorted by date in WeeklyRecord.calculateAverages()
                loadPlantImage(holder, weekly.getDailyRecords().get(weekly.getDailyRecords().size() - 1));
            } else {
                holder.ivPlant.setImageResource(R.drawable.lettuce);
                holder.ivPlant.setOnClickListener(null);
            }

        } else if (item instanceof GrowthRecord) {
            GrowthRecord record = (GrowthRecord) item;
            holder.tvDateTime.setText(record.getDate());

            holder.rlFooter.setVisibility(View.GONE);

            GrowthRecord.DailyAverage avg = record.getDailyAverage();
            if (avg != null) {
                holder.tvDay.setText(avg.getDay() != null ? avg.getDay() : holder.itemView.getContext().getString(R.string.day));
                holder.tvTemp.setText(String.format(Locale.getDefault(), "%.1f °C", avg.getAvgTemperature()));
                holder.tvHumidity.setText(String.format(Locale.getDefault(), "%.0f%%", avg.getAvgHumidity()));
                holder.tvPh.setText(String.format(Locale.getDefault(), "%.1f", avg.getAvgPh()));
                holder.tvGrowthSize.setText(String.format(Locale.getDefault(), "%.1f cm", avg.getAvgGrowthSize()));

                loadPlantImage(holder, record);
            } else {
                holder.tvDay.setText(holder.itemView.getContext().getString(R.string.day));
                holder.tvTemp.setText("N/A");
                holder.tvHumidity.setText("N/A");
                holder.tvPh.setText("N/A");
                holder.tvGrowthSize.setText("N/A");
                holder.ivPlant.setImageResource(R.drawable.lettuce);
                holder.ivPlant.setOnClickListener(null);
            }
        }

        holder.tvDuration.setVisibility(View.GONE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    private void loadPlantImage(ViewHolder holder, GrowthRecord record) {

        GrowthRecord.DailyAverage avg = record.getDailyAverage();

        if (avg == null) {
            holder.ivPlant.setImageResource(R.drawable.lettuce);
            return;
        }

        // Temporary placeholder while loading
        holder.ivPlant.setImageResource(R.drawable.lettuce);

        if (greenhouseId == null ||
                batchName == null ||
                avg.getDay() == null) {

            loadDirectUrl(holder, avg.getImageUrl());
            return;
        }

        // Example:
        // "Day 1" -> "1"
        String dayNumberString =
                avg.getDay().replaceAll("[^0-9]", "");

        if (dayNumberString.isEmpty()) {
            loadDirectUrl(holder, avg.getImageUrl());
            return;
        }

        String cleanBatch =
                batchName.replace(" ", "");

        // REAL STORAGE PATH:
        // growthImage/GH001/Batch4/day1_processed.jpg
        String storagePath =
                "growthImage/"
                        + greenhouseId
                        + "/"
                        + cleanBatch
                        + "/day"
                        + dayNumberString
                        + "_processed.jpg";

        android.util.Log.d(
                "GrowthImage",
                "Trying Firebase Storage: " + storagePath
        );

        StorageReference imageReference =
                FirebaseStorage
                        .getInstance()
                        .getReference()
                        .child(storagePath);

        imageReference
                .getDownloadUrl()
                .addOnSuccessListener(uri -> {

                    android.util.Log.d(
                            "GrowthImage",
                            "Image URL retrieved: " + uri
                    );

                    Glide.with(holder.itemView.getContext())
                            .load(uri)
                            .placeholder(R.drawable.lettuce)
                            .error(R.drawable.lettuce)
                            .centerCrop()
                            .into(holder.ivPlant);
                    
                    holder.ivPlant.setOnClickListener(v -> showFullImage(v.getContext(), uri));

                })
                .addOnFailureListener(e -> {

                    android.util.Log.e(
                            "GrowthImage",
                            "Storage image failed: "
                                    + storagePath,
                            e
                    );

                    // Try Firestore imageUrl only as fallback
                    loadDirectUrl(
                            holder,
                            avg.getImageUrl()
                    );
                });
    }

    private void loadDirectUrl(
            ViewHolder holder,
            String imageUrl
    ) {

        if (imageUrl == null ||
                imageUrl.trim().isEmpty()) {

            holder.ivPlant.setImageResource(
                    R.drawable.lettuce
            );
            holder.ivPlant.setOnClickListener(null);

            return;
        }

        Glide.with(holder.itemView.getContext())
                .load(imageUrl)
                .placeholder(R.drawable.lettuce)
                .error(R.drawable.lettuce)
                .centerCrop()
                .into(holder.ivPlant);

        holder.ivPlant.setOnClickListener(v -> showFullImage(v.getContext(), imageUrl));
    }

    private void showFullImage(android.content.Context context, Object source) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_full_image, null);
        ImageView fullImageView = dialogView.findViewById(R.id.fullImageView);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);

        Glide.with(context)
                .load(source)
                .into(fullImageView);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        fullImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void setupSparkline(LineChart chart, List<Float> data) {
        if (data == null || data.isEmpty()) {
            chart.setVisibility(View.INVISIBLE);
            return;
        }
        chart.setVisibility(View.VISIBLE);

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            entries.add(new Entry(i, data.get(i)));
        }

        LineDataSet set = new LineDataSet(entries, "");
        int activeColor = ContextCompat.getColor(chart.getContext(), R.color.recommendation_text);
        int fillColor = ContextCompat.getColor(chart.getContext(), R.color.status_improved_bg);

        set.setColor(activeColor);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setDrawFilled(true);
        set.setFillColor(fillColor);

        LineData lineData = new LineData(set);
        chart.setData(lineData);

        chart.getLegend().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getXAxis().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.invalidate();
    }

    private List<Float> getSparklineDataForWeekly(WeeklyRecord weekly) {
        // This is a placeholder. In a real scenario, you'd extract this from weekly.getDailyRecords()
        List<Float> data = new ArrayList<>();
        data.add(1.0f); data.add(1.2f); data.add(1.1f); data.add(1.5f); 
        data.add(1.4f); data.add(1.8f); data.add(2.0f);
        return data;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDay, tvDateTime, tvTemp, tvHumidity, tvPh, tvDuration, tvGrowthSize;
        ProgressBar pbGrowth;
        ImageView ivPlant;
        View btnViewDetails, rlFooter;
        LineChart sparklineChart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            tvHumidity = itemView.findViewById(R.id.tvHumidity);
            tvPh = itemView.findViewById(R.id.tvPh);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvGrowthSize = itemView.findViewById(R.id.tvGrowthSize);
            pbGrowth = itemView.findViewById(R.id.pbGrowth);
            ivPlant = itemView.findViewById(R.id.ivPlant);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            rlFooter = itemView.findViewById(R.id.rlFooter);
            sparklineChart = itemView.findViewById(R.id.sparklineChart);
        }
    }
}
