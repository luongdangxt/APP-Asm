package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;

class BudgetRepository {
    private final BudgetDao budgetDao;

    BudgetRepository(Context context) {
        this.budgetDao = AppDatabase.getInstance(context).budgetDao();
    }

    long upsert(long userId, int monthKey, String category, double limit) {
        Budget existing = budgetDao.find(userId, monthKey, category);
        if (existing != null) {
            existing.limitAmount = limit;
            budgetDao.update(existing);
            return existing.id;
        }
        return budgetDao.upsert(new Budget(userId, monthKey, category, limit));
    }

    List<Budget> listForMonth(long userId, int monthKey) {
        return budgetDao.listForMonth(userId, monthKey);
    }
}
