package com.example.personalfinancialmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CombinedEntryAdapter extends ListAdapter<CombinedEntryAdapter.Item, CombinedEntryAdapter.VH> {
    public static class Item {
        boolean income; long time; String title; double amount; String category;
        Item(boolean income, long time, String title, double amount, String category){ this.income=income; this.time=time; this.title=title; this.amount=amount; this.category=category; }
    }

    private static final DiffUtil.ItemCallback<Item> DIFF = new DiffUtil.ItemCallback<Item>() {
        @Override public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.income == newItem.income && oldItem.time == newItem.time &&
                    ((oldItem.title == null && newItem.title == null) ||
                            (oldItem.title != null && oldItem.title.equals(newItem.title)));
        }
        @Override public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
            return oldItem.amount == newItem.amount &&
                    ((oldItem.category == null && newItem.category == null) ||
                            (oldItem.category != null && oldItem.category.equals(newItem.category)));
        }
    };

    private final SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    public CombinedEntryAdapter(){ super(DIFF); }
    public void submit(List<Item> data){ submitList(data); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse existing entry item layout
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense_entry, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = getItem(position);
        h.title.setText(it.title);
        h.date.setText(df.format(new Date(it.time)));
        String sign = it.income ? "+" : "-";
        h.amount.setText(String.format(Locale.getDefault(), "%s $%,.2f", sign, it.amount));
        h.meta.setText(it.category);
        int icon = it.income ? R.drawable.ic_bag : R.drawable.ic_bike; // simple fallback; could map category to icons
        h.icon.setImageResource(icon);
    }
    static class VH extends RecyclerView.ViewHolder {
        ImageView icon; TextView title; TextView date; TextView amount; TextView meta;
        VH(View v){ super(v); icon=v.findViewById(R.id.iv_icon); title=v.findViewById(R.id.tv_desc); date=v.findViewById(R.id.tv_date); amount=v.findViewById(R.id.tv_amount); meta=v.findViewById(R.id.tv_meta);} }
}
