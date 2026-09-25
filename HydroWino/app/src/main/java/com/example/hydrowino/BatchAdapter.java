package com.example.hydrowino;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
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

public class BatchAdapter extends RecyclerView.Adapter<BatchAdapter.ViewHolder> {

    private List<PlantBatch> batches;

    public BatchAdapter(List<PlantBatch> batches) {
        this.batches = batches;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_batch, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PlantBatch batch = batches.get(position);

        holder.tvBatchName.setText(batch.getName());
        holder.tvStatus.setText(batch.getStatus());
        holder.tvPlantType.setText(batch.getType());
        holder.tvDateRange.setText(String.format("%s (%s)", batch.getDateRange(), batch.getDuration()));
        
        holder.tvAvgTemp.setText(String.format(Locale.getDefault(), "%.1f °C", batch.getAvgTemp()));
        holder.tvAvgPh.setText(String.format(Locale.getDefault(), "%.1f", batch.getAvgPh()));
        holder.tvAvgHumidity.setText(String.format(Locale.getDefault(), "%.0f%%", batch.getAvgHumidity()));

        Glide.with(holder.itemView.getContext())
                .load(batch.getImageUrl())
                .placeholder(R.drawable.lettuce)
                .into(holder.ivPlant);

        setupSparkline(holder.sparklineChart, batch.getSparklineData());

        holder.btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), BatchDetailsActivity.class);
            intent.putExtra("batch", batch);
            holder.itemView.getContext().startActivity(intent);
        });
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

    @Override
    public int getItemCount() {
        return batches.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPlant;
        TextView tvBatchName, tvStatus, tvPlantType, tvDateRange;
        TextView tvAvgTemp, tvAvgPh, tvAvgHumidity;
        LineChart sparklineChart;
        LinearLayout btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPlant = itemView.findViewById(R.id.ivPlant);
            tvBatchName = itemView.findViewById(R.id.tvBatchName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPlantType = itemView.findViewById(R.id.tvPlantType);
            tvDateRange = itemView.findViewById(R.id.tvDateRange);
            tvAvgTemp = itemView.findViewById(R.id.tvAvgTemp);
            tvAvgPh = itemView.findViewById(R.id.tvAvgPh);
            tvAvgHumidity = itemView.findViewById(R.id.tvAvgHumidity);
            sparklineChart = itemView.findViewById(R.id.sparklineChart);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}
