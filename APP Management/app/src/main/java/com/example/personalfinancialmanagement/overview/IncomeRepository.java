package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;

class IncomeRepository {
    private final IncomeDao incomeDao;

    IncomeRepository(Context context) {
        this.incomeDao = AppDatabase.getInstance(context).incomeDao();
    }

    long add(Income income) { return incomeDao.insert(income); }
    List<Income> latest(long userId, int limit) { return incomeDao.latest(userId, limit); }
    List<Income> listForCurrentMonth(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        return incomeDao.listByDateRange(userId, start, end);
    }

    List<Income> listForDay(long userId, long dayTime) {
        long start = MonthUtils.dayStartUtcMillis(dayTime);
        long end = MonthUtils.dayEndUtcMillis(dayTime);
        return incomeDao.listByDateRange(userId, start, end);
    }
}
