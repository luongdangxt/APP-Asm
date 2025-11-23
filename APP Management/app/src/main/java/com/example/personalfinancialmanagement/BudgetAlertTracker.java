package com.example.personalfinancialmanagement;

import android.content.Context;
import android.content.SharedPreferences;

public class BudgetAlertTracker {
    private static final String PREFS = "budget_alert_tracker";
    private final SharedPreferences prefs;

    public BudgetAlertTracker(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    int getLastThreshold(long userId, int monthKey, String category) {
        return prefs.getInt(key(userId, monthKey, category), 0);
    }

    void setLastThreshold(long userId, int monthKey, String category, int threshold) {
        prefs.edit().putInt(key(userId, monthKey, category), threshold).apply();
    }

    public void reset(long userId, int monthKey, String category) {
        prefs.edit().remove(key(userId, monthKey, category)).apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }

    private String key(long userId, int monthKey, String category) {
        String normalized = CategoryPreferences.sanitizeLabel(category == null ? "" : category);
        return userId + ":" + monthKey + ":" + normalized;
    }
}
