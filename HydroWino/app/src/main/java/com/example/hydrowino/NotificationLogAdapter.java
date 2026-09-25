package com.example.hydrowino;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.LogViewHolder> {

    private List<NotificationLog> logs;
    private OnLogClickListener listener;

    public interface OnLogClickListener {
        void onLogClick(NotificationLog log);
        void onDeleteClick(NotificationLog log);
    }

    public NotificationLogAdapter(List<NotificationLog> logs, OnLogClickListener listener) {
        this.logs = logs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        NotificationLog log = logs.get(position);
        Context context = holder.itemView.getContext();

        // Get Localized Strings based on Type
        String title = log.getTitle();
        String message = log.getMessage();
        String type = log.getType();

        if (type != null) {
            switch (type) {
                case "water":
                    title = context.getString(R.string.alert_water_title);
                    message = context.getString(R.string.alert_water_overflow_msg);
                    holder.ivIcon.setImageResource(R.drawable.drop1);
                    break;
                case "temperature":
                    title = context.getString(R.string.alert_temp_title);
                    // Decide which message based on content or specific sub-types if we had them
                    if (message.contains("high")) message = context.getString(R.string.alert_temp_high_msg);
                    else if (message.contains("cold")) message = context.getString(R.string.alert_temp_low_msg);
                    holder.ivIcon.setImageResource(R.drawable.ic_temperature);
                    break;
                case "humidity":
                    title = context.getString(R.string.alert_humid_title);
                    if (message.contains("high")) message = context.getString(R.string.alert_humid_high_msg);
                    else if (message.contains("low")) message = context.getString(R.string.alert_humid_low_msg);
                    holder.ivIcon.setImageResource(R.drawable.humidity);
                    break;
                case "ph":
                    title = context.getString(R.string.alert_ph_title);
                    if (message.contains("acidic")) message = context.getString(R.string.alert_ph_acidic_msg);
                    else if (message.contains("alkaline")) message = context.getString(R.string.alert_ph_alkaline_msg);
                    holder.ivIcon.setImageResource(R.drawable.phsensor24);
                    break;
                default:
                    holder.ivIcon.setImageResource(R.drawable.ic_warning);
                    break;
            }
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_warning);
        }

        holder.tvTitle.setText(title);
        holder.tvMessage.setText(message);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateString = sdf.format(new Date(log.getTimestamp()));
        holder.tvTimestamp.setText(dateString);

        // Highlight unread notifications
        if (!log.isRead()) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_improved_bg));
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.status_improved_bg));
            holder.unreadStripe.setVisibility(View.VISIBLE);
            holder.unreadDot.setVisibility(View.VISIBLE);
            holder.statusBadge.setText(context.getString(R.string.unread));
            holder.statusBadge.setBackgroundResource(R.drawable.badge_unread_bg);
            holder.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_improved_text));
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
        } else {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_background));
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.divider_color));
            holder.unreadStripe.setVisibility(View.GONE);
            holder.unreadDot.setVisibility(View.GONE);
            holder.statusBadge.setText(context.getString(R.string.read));
            holder.statusBadge.setBackgroundResource(R.drawable.badge_read_bg);
            holder.statusBadge.setTextColor(ContextCompat.getColor(context, R.color.status_stable_text));
            holder.tvTitle.setTypeface(null, Typeface.BOLD);
        }

        holder.itemView.setOnClickListener(v -> listener.onLogClick(log));
        
        // Use long click for delete to keep UI clean like image
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(log);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTimestamp, statusBadge;
        ImageView ivIcon, ivDelete, ivChevron;
        View unreadStripe, unreadDot;
        MaterialCardView cardView;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvLogTitle);
            tvMessage = itemView.findViewById(R.id.tvLogMessage);
            tvTimestamp = itemView.findViewById(R.id.tvLogTimestamp);
            statusBadge = itemView.findViewById(R.id.tvLogStatusBadge);
            ivIcon = itemView.findViewById(R.id.ivLogIcon);
            ivDelete = itemView.findViewById(R.id.ivDeleteLog);
            ivChevron = itemView.findViewById(R.id.ivChevron);
            unreadStripe = itemView.findViewById(R.id.viewUnreadStripe);
            unreadDot = itemView.findViewById(R.id.viewUnreadDot);
            cardView = itemView.findViewById(R.id.cardLogRoot);
        }
    }
}