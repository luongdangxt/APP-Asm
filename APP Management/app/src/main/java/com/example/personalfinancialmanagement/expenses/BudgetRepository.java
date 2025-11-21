package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;

public class BudgetRepository {
    private final BudgetDao budgetDao;

    public BudgetRepository(Context context) {
        this.budgetDao = AppDatabase.getInstance(context).budgetDao();
    }

    public long upsert(long userId, int monthKey, String category, double limit) {
        Budget existing = budgetDao.find(userId, monthKey, category);
        if (existing != null) {
            existing.limitAmount = limit;
            budgetDao.update(existing);
            return existing.id;
        }
        return budgetDao.upsert(new Budget(userId, monthKey, category, limit));
    }

    public List<Budget> listForMonth(long userId, int monthKey) {
        return budgetDao.listForMonth(userId, monthKey);
    }

    public Budget find(long userId, int monthKey, String category) {
        String normalized = CategoryPreferences.sanitizeLabel(category);
        if (normalized.isEmpty()) return null;
        return budgetDao.find(userId, monthKey, normalized);
    }

    public void delete(long id) {
        if (id <= 0) return;
        budgetDao.delete(id);
    }
}
