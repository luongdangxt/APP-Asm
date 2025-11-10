package com.example.personalfinancialmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import java.util.ArrayList;
import java.util.List;

class WeekDayAdapter extends RecyclerView.Adapter<WeekDayAdapter.VH> {
    static class Day {
        int dayOfMonth; boolean inMonth; boolean selected;
        long timeMillis;
        Day(int d, boolean inMonth, boolean selected, long timeMillis){ this.dayOfMonth=d; this.inMonth=inMonth; this.selected=selected; this.timeMillis=timeMillis; }
    }

    interface OnDayClick { void onClick(Day d); }

    private final List<Day> days = new ArrayList<>();
    private OnDayClick listener;
    private boolean colorsInit = false;
    private int colorPrimaryText;
    private int colorSecondaryText;
    private int colorOutMonth;

    void setListener(OnDayClick l){ this.listener = l; }
    void submit(List<Day> data){ days.clear(); if (data!=null) days.addAll(data); notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (!colorsInit) {
            colorPrimaryText = ContextCompat.getColor(parent.getContext(), R.color.textPrimary);
            colorSecondaryText = ContextCompat.getColor(parent.getContext(), R.color.textSecondary);
            colorOutMonth = ColorUtils.setAlphaComponent(colorSecondaryText, 120); // ~47% alpha
            colorsInit = true;
        }
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_week_day, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Day d = days.get(position);
        h.tv.setText(String.valueOf(d.dayOfMonth));
        if (d.selected) {
            h.tv.setBackgroundResource(R.drawable.bg_day_selected);
            h.tv.setTextColor(0xFFFFFFFF);
        } else {
            h.tv.setBackground(null);
            h.tv.setTextColor(d.inMonth ? colorPrimaryText : colorOutMonth);
        }
        h.itemView.setOnClickListener(v -> { if (listener!=null) listener.onClick(d); });
    }
    @Override public int getItemCount() { return days.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tv; VH(@NonNull View itemView){ super(itemView); tv=itemView.findViewById(R.id.tv_day);} }
}
