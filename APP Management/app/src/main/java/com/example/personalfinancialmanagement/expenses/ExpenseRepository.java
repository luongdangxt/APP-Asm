package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;

class ExpenseRepository {
    private final ExpenseDao expenseDao;

    ExpenseRepository(Context context) {
        this.expenseDao = AppDatabase.getInstance(context).expenseDao();
    }

    long add(Expense e) { return expenseDao.insert(e); }
    List<Expense> listForCurrentMonth(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        return expenseDao.listByDateRange(userId, start, end);
    }
    List<Expense> listForDay(long userId, long dayTime) {
        long start = MonthUtils.dayStartUtcMillis(dayTime);
        long end = MonthUtils.dayEndUtcMillis(dayTime);
        return expenseDao.listByDateRange(userId, start, end);
    }
    List<Expense> latest(long userId, int limit) { return expenseDao.latest(userId, limit); }
}
