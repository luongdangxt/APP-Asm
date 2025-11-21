package com.example.personalfinancialmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Displays category, amount and percent share. */
class CategorySummaryAdapter extends RecyclerView.Adapter<CategorySummaryAdapter.VH> {
    static class Item { String name; double total; int percent; Item(String n,double t,int p){name=n;total=t;percent=p;} }
    private final List<Item> items = new ArrayList<>();
    void submit(List<Item> data){ items.clear(); if (data!=null) items.addAll(data); notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_stat, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = items.get(position);
        h.name.setText(it.name);
        h.amount.setText(String.format(Locale.getDefault(), "$%,.2f", it.total));
        h.meta.setText(it.percent + "%");
    }
    @Override public int getItemCount(){ return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, amount, meta; VH(View v){ super(v); name=v.findViewById(R.id.tv_cat_name); amount=v.findViewById(R.id.tv_cat_amount); meta=v.findViewById(R.id.tv_cat_meta);} }
}

