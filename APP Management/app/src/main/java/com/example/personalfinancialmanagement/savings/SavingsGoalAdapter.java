package com.example.personalfinancialmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

class SavingsGoalAdapter extends RecyclerView.Adapter<SavingsGoalAdapter.VH> {
    static class Item {
        final SavingsGoal goal;
        final double current;
        Item(SavingsGoal g, double c) { goal = g; current = c; }
    }

    interface OnMoreClick { void onMore(SavingsGoal goal); }

    private final List<Item> items = new ArrayList<>();
    private final OnMoreClick moreClick;

    SavingsGoalAdapter(OnMoreClick moreClick) {
        this.moreClick = moreClick;
        setHasStableIds(true);
    }

    void submit(List<Item> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_savings_goal, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int i) {
        Item it = items.get(i);
        h.title.setText(it.goal.title);
        h.current.setText(String.format(java.util.Locale.getDefault(), "$%,.0f saved", it.current));
        h.target.setText(String.format(java.util.Locale.getDefault(), "Target: $%,.0f", it.goal.targetAmount));
        int pct = it.goal.targetAmount <= 0 ? 0 : (int) Math.min(100, Math.round(it.current * 100 / it.goal.targetAmount));
        h.progress.setProgress(pct);
        h.percent.setText(String.format(java.util.Locale.getDefault(), "%d%%", pct));
        double remaining = Math.max(0, it.goal.targetAmount - it.current);
        h.remaining.setText(String.format(java.util.Locale.getDefault(), "%s remaining", String.format(java.util.Locale.getDefault(), "$%,.0f", remaining)));

        // icon mapping by simple keys
        int res = R.drawable.ic_bag;
        if ("bike".equals(it.goal.iconKey)) res = R.drawable.ic_bike;
        if ("phone".equals(it.goal.iconKey)) res = R.drawable.ic_task; // placeholder
        h.icon.setImageResource(res);

        h.btnMore.setOnClickListener(v -> moreClick.onMore(it.goal));
    }

    @Override public int getItemCount() { return items.size(); }
    @Override public long getItemId(int position) { return items.get(position).goal.id; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon; TextView title; ProgressBar progress; TextView current; TextView target; TextView percent; TextView remaining; ImageButton btnMore;
        VH(@NonNull View v) {
            super(v);
            icon = v.findViewById(R.id.iv_icon);
            title = v.findViewById(R.id.tv_title);
            progress = v.findViewById(R.id.progress);
            current = v.findViewById(R.id.tv_current);
            target = v.findViewById(R.id.tv_target);
            percent = v.findViewById(R.id.tv_percent);
            remaining = v.findViewById(R.id.tv_remaining);
            btnMore = v.findViewById(R.id.btn_more);
        }
    }
}
