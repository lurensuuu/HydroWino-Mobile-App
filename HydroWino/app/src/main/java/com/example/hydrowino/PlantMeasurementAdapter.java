package com.example.hydrowino;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class PlantMeasurementAdapter extends RecyclerView.Adapter<PlantMeasurementAdapter.ViewHolder> {

    private List<GrowthRecord.PlantMeasurement> plantList;

    public PlantMeasurementAdapter(List<GrowthRecord.PlantMeasurement> plantList) {
        this.plantList = plantList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plant_measurement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GrowthRecord.PlantMeasurement plant = plantList.get(position);
        
        holder.tvPlantName.setText("Plant " + plant.getPlantId());
        
        holder.tvLengthCm.setText(String.format(Locale.getDefault(), "%.2f cm", plant.getLengthCm()));
        holder.tvLengthIn.setText(String.format(Locale.getDefault(), "%.2f in", plant.getLengthInches()));
        
        holder.tvWidthCm.setText(String.format(Locale.getDefault(), "%.2f cm", plant.getWidthCm()));
        holder.tvWidthIn.setText(String.format(Locale.getDefault(), "%.2f in", plant.getWidthInches()));
    }

    @Override
    public int getItemCount() {
        return plantList != null ? plantList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlantName, tvLengthCm, tvLengthIn, tvWidthCm, tvWidthIn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlantName = itemView.findViewById(R.id.tvPlantName);
            tvLengthCm = itemView.findViewById(R.id.tvLengthCm);
            tvLengthIn = itemView.findViewById(R.id.tvLengthIn);
            tvWidthCm = itemView.findViewById(R.id.tvWidthCm);
            tvWidthIn = itemView.findViewById(R.id.tvWidthIn);
        }
    }
}
