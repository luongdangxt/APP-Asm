package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;
import java.util.ArrayList;

import com.example.personalfinancialmanagement.network.FinanceRemoteRepository;

public class ExpenseRepository {
    private final ExpenseDao expenseDao;
    private final FinanceRemoteRepository remote;

    public ExpenseRepository(Context context) {
        this.expenseDao = AppDatabase.getInstance(context).expenseDao();
        this.remote = new FinanceRemoteRepository(context);
    }

    public long add(Expense e) {
        boolean ok = remote.addExpense(e.userId, e);
        // cache locally for offline use
        if (ok) {
            expenseDao.insert(e);
            return e.id;
        }
        return expenseDao.insert(e);
    }
    public List<Expense> listForCurrentMonth(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        List<Expense> remoteList = remote.listExpenses(userId);
        if (remoteList != null) {
            List<Expense> filtered = new ArrayList<>();
            for (Expense ex : remoteList) {
                if (ex.dateUtc >= start && ex.dateUtc <= end) filtered.add(ex);
            }
            if (!filtered.isEmpty()) return filtered;
        }
        return expenseDao.listByDateRange(userId, start, end);
    }
    public List<Expense> listForDay(long userId, long dayTime) {
        long start = MonthUtils.dayStartUtcMillis(dayTime);
        long end = MonthUtils.dayEndUtcMillis(dayTime);
        List<Expense> remoteList = remote.listExpenses(userId);
        if (remoteList != null) {
            List<Expense> filtered = new ArrayList<>();
            for (Expense ex : remoteList) if (ex.dateUtc >= start && ex.dateUtc <= end) filtered.add(ex);
            if (!filtered.isEmpty()) return filtered;
        }
        return expenseDao.listByDateRange(userId, start, end);
    }
    public List<Expense> latest(long userId, int limit) {
        List<Expense> remoteList = remote.listExpenses(userId);
        if (remoteList != null && !remoteList.isEmpty()) {
            remoteList.sort((a,b)->Long.compare(b.dateUtc, a.dateUtc));
            return remoteList.subList(0, Math.min(limit, remoteList.size()));
        }
        return expenseDao.latest(userId, limit);
    }

    public double sumForCategory(long userId, String category, int monthKey) {
        if (userId <= 0 || category == null) return 0;
        String normalized = CategoryPreferences.sanitizeLabel(category);
        if (normalized.isEmpty()) return 0;
        String key = MonthUtils.monthKeyString(monthKey);
        if (key.isEmpty()) return 0;
        List<Expense> remoteList = remote.listExpenses(userId);
        double sum = 0;
        if (remoteList != null && !remoteList.isEmpty()) {
            long[] range = monthRangeFromKey(monthKey);
            long start = range[0], end = range[1];
            for (Expense e : remoteList) {
                if (e.category != null && CategoryPreferences.sanitizeLabel(e.category).equals(normalized)
                        && e.dateUtc >= start && e.dateUtc <= end) {
                    sum += e.amount;
                }
            }
            return sum;
        }
        return expenseDao.sumByCategoryForMonth(userId, normalized, key);
    }

    private long[] monthRangeFromKey(int monthKey) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int year = monthKey / 100;
        int month = monthKey % 100;
        c.clear();
        c.set(java.util.Calendar.YEAR, year);
        c.set(java.util.Calendar.MONTH, Math.max(0, month - 1));
        c.set(java.util.Calendar.DAY_OF_MONTH, 1);
        long start = c.getTimeInMillis();
        c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        c.set(java.util.Calendar.HOUR_OF_DAY, 23);
        c.set(java.util.Calendar.MINUTE, 59);
        c.set(java.util.Calendar.SECOND, 59);
        c.set(java.util.Calendar.MILLISECOND, 999);
        long end = c.getTimeInMillis();
        return new long[]{start, end};
    }
}
