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

class CategoryStatAdapter extends RecyclerView.Adapter<CategoryStatAdapter.VH> {
    static class Item { String category; double total; int count; Item(String c,double t,int n){category=c;total=t;count=n;} }
    private final List<Item> items = new ArrayList<>();
    void submit(List<Item> data){ items.clear(); if (data!=null) items.addAll(data); notifyDataSetChanged(); }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_stat, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        Item it = items.get(position);
        h.name.setText(it.category);
        h.amount.setText(String.format(Locale.getDefault(), "$%,.2f", it.total));
        h.meta.setText(it.count + " items");
    }
    @Override public int getItemCount(){ return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name, amount, meta; VH(View v){ super(v); name=v.findViewById(R.id.tv_cat_name); amount=v.findViewById(R.id.tv_cat_amount); meta=v.findViewById(R.id.tv_cat_meta);} }
}
