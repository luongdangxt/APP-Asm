package com.example.personalfinancialmanagement;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Stores user-defined income sources and expense categories so they can be reused.
 */
public class CategoryPreferences {
    private static final String PREFS_NAME = "category_prefs";
    private static final String KEY_EXPENSE = "custom_expense_categories";
    private static final String KEY_INCOME = "custom_income_sources";

    private final SharedPreferences prefs;

    public CategoryPreferences(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<String> getCustomExpenseCategories() {
        return new ArrayList<>(getValues(KEY_EXPENSE));
    }

    public List<String> getCustomIncomeSources() {
        return new ArrayList<>(getValues(KEY_INCOME));
    }

    public boolean addCustomExpenseCategory(String value) {
        return addValue(KEY_EXPENSE, value);
    }

    public boolean addCustomIncomeSource(String value) {
        return addValue(KEY_INCOME, value);
    }

    public boolean removeCustomExpenseCategory(String value) {
        return removeValue(KEY_EXPENSE, value);
    }

    public boolean removeCustomIncomeSource(String value) {
        return removeValue(KEY_INCOME, value);
    }

    public boolean renameCustomExpenseCategory(String oldValue, String newValue) {
        return replaceValue(KEY_EXPENSE, oldValue, newValue);
    }

    public boolean renameCustomIncomeSource(String oldValue, String newValue) {
        return replaceValue(KEY_INCOME, oldValue, newValue);
    }

    private Set<String> getValues(String key) {
        Set<String> stored = prefs.getStringSet(key, Collections.emptySet());
        return new LinkedHashSet<>(stored);
    }

    private boolean addValue(String key, String value) {
        String normalized = sanitizeLabel(value);
        if (normalized.isEmpty()) return false;
        Set<String> copy = getValues(key);
        boolean added = copy.add(normalized);
        if (added) {
            prefs.edit().putStringSet(key, copy).apply();
        }
        return added;
    }

    private boolean removeValue(String key, String value) {
        String normalized = sanitizeLabel(value);
        if (normalized.isEmpty()) return false;
        Set<String> copy = getValues(key);
        boolean removed = copy.remove(normalized);
        if (removed) {
            prefs.edit().putStringSet(key, copy).apply();
        }
        return removed;
    }

    private boolean replaceValue(String key, String oldValue, String newValue) {
        String oldNorm = sanitizeLabel(oldValue);
        String newNorm = sanitizeLabel(newValue);
        if (oldNorm.isEmpty() || newNorm.isEmpty()) return false;
        Set<String> copy = getValues(key);
        if (!copy.remove(oldNorm)) return false;
        copy.add(newNorm);
        prefs.edit().putStringSet(key, copy).apply();
        return true;
    }

    public static String sanitizeLabel(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return "";
        // collapse multiple spaces and capitalize first letter of each word
        trimmed = trimmed.replaceAll("\\s+", " ");
        String[] parts = trimmed.split(" ");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;
            if (builder.length() > 0) builder.append(' ');
            if (part.length() == 1) builder.append(part.toUpperCase(Locale.getDefault()));
            else builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    public void clearAll() {
        prefs.edit()
                .remove(KEY_EXPENSE)
                .remove(KEY_INCOME)
                .apply();
    }
}
