package com.example.personalfinancialmanagement;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.Holder> {
    private final List<OnboardingPage> pages;
    OnboardingAdapter(List<OnboardingPage> pages) { this.pages = pages; }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        OnboardingPage p = pages.get(position);
        h.image.setImageResource(R.drawable.ic_logo); // only logo shown
        h.title.setText(p.title);
        h.subtitle.setText(p.subtitle);
    }

    @Override
    public int getItemCount() { return pages.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView image; final TextView title; final TextView subtitle;
        Holder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.logo);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
        }
    }
}
