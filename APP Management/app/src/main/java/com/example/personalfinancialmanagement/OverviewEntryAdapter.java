package com.example.personalfinancialmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class OverviewEntryAdapter extends RecyclerView.Adapter<OverviewEntryAdapter.VH> {
    private final List<Expense> items = new ArrayList<>();
    private final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    void submit(List<Expense> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense_entry, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Expense e = items.get(position);
        h.tvDesc.setText(e.description);
        h.tvDate.setText(df.format(new Date(e.dateUtc)));
        h.tvAmount.setText(String.format(Locale.getDefault(), "$%.2f", e.amount));
        h.tvMeta.setText(e.category);
        int icon = R.drawable.ic_bag;
        String cat = e.category == null ? "" : e.category.toLowerCase(java.util.Locale.getDefault());
        if (cat.contains("food") || cat.contains("meal") || cat.contains("eat")) icon = R.drawable.ic_food;
        else if (cat.contains("transport") || cat.contains("uber") || cat.contains("taxi") || cat.contains("bike")) icon = R.drawable.ic_bike;
        else if (cat.contains("shop") || cat.contains("market") || cat.contains("grocer")) icon = R.drawable.ic_bag;
        h.ivIcon.setImageResource(icon);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivIcon; TextView tvDesc; TextView tvDate; TextView tvAmount; TextView tvMeta;
        VH(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvDesc = itemView.findViewById(R.id.tv_desc);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvAmount = itemView.findViewById(R.id.tv_amount);
            tvMeta = itemView.findViewById(R.id.tv_meta);
        }
    }
}
