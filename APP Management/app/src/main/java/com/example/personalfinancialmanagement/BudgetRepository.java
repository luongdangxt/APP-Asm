package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;
import java.util.ArrayList;

import com.example.personalfinancialmanagement.network.FinanceRemoteRepository;

public class BudgetRepository {
    private final BudgetDao budgetDao;
    private final FinanceRemoteRepository remote;

    public BudgetRepository(Context context) {
        this.budgetDao = AppDatabase.getInstance(context).budgetDao();
        this.remote = new FinanceRemoteRepository(context);
    }

    public long upsert(long userId, int monthKey, String category, double limit) {
        boolean ok = remote.upsertBudget(userId, monthKey, category, limit);
        if (ok) {
            Budget existing = budgetDao.find(userId, monthKey, category);
            if (existing != null) {
                existing.limitAmount = limit;
                budgetDao.update(existing);
                return existing.id;
            }
            return budgetDao.upsert(new Budget(userId, monthKey, category, limit));
        }
        Budget existing = budgetDao.find(userId, monthKey, category);
        if (existing != null) {
            existing.limitAmount = limit;
            budgetDao.update(existing);
            return existing.id;
        }
        return budgetDao.upsert(new Budget(userId, monthKey, category, limit));
    }

    public List<Budget> listForMonth(long userId, int monthKey) {
        List<Budget> remoteList = remote.listBudgets(userId, monthKey);
        if (remoteList != null && !remoteList.isEmpty()) return remoteList;
        return budgetDao.listForMonth(userId, monthKey);
    }

    public Budget find(long userId, int monthKey, String category) {
        String normalized = CategoryPreferences.sanitizeLabel(category);
        if (normalized.isEmpty()) return null;
        List<Budget> remoteList = remote.listBudgets(userId, monthKey);
        if (remoteList != null) {
            for (Budget b : remoteList) {
                if (CategoryPreferences.sanitizeLabel(b.category).equals(normalized)) return b;
            }
        }
        return budgetDao.find(userId, monthKey, normalized);
    }

    public void delete(long userId, int monthKey, String category) {
        String normalized = CategoryPreferences.sanitizeLabel(category);
        if (normalized.isEmpty()) return;
        remote.deleteBudgetComposite(monthKey, normalized);
        Budget existing = budgetDao.find(userId, monthKey, normalized);
        if (existing != null) {
            budgetDao.delete(existing.id);
        }
    }
}
