package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;
import java.util.ArrayList;

import com.example.personalfinancialmanagement.network.FinanceRemoteRepository;

public class IncomeRepository {
    private final IncomeDao incomeDao;
    private final FinanceRemoteRepository remote;

    public IncomeRepository(Context context) {
        this.incomeDao = AppDatabase.getInstance(context).incomeDao();
        this.remote = new FinanceRemoteRepository(context);
    }

    public long add(Income income) {
        boolean ok = remote.addIncome(income.userId, income);
        if (ok) {
            incomeDao.insert(income);
            return income.id;
        }
        return incomeDao.insert(income);
    }
    public List<Income> latest(long userId, int limit) {
        List<Income> remoteList = remote.listIncomes(userId);
        if (remoteList != null && !remoteList.isEmpty()) {
            remoteList.sort((a,b)->Long.compare(b.dateUtc, a.dateUtc));
            return remoteList.subList(0, Math.min(limit, remoteList.size()));
        }
        return incomeDao.latest(userId, limit);
    }
    public List<Income> listForCurrentMonth(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        List<Income> remoteList = remote.listIncomes(userId);
        if (remoteList != null) {
            List<Income> filtered = new ArrayList<>();
            for (Income in : remoteList) if (in.dateUtc >= start && in.dateUtc <= end) filtered.add(in);
            if (!filtered.isEmpty()) return filtered;
        }
        return incomeDao.listByDateRange(userId, start, end);
    }

    public List<Income> listForDay(long userId, long dayTime) {
        long start = MonthUtils.dayStartUtcMillis(dayTime);
        long end = MonthUtils.dayEndUtcMillis(dayTime);
        List<Income> remoteList = remote.listIncomes(userId);
        if (remoteList != null) {
            List<Income> filtered = new ArrayList<>();
            for (Income in : remoteList) if (in.dateUtc >= start && in.dateUtc <= end) filtered.add(in);
            if (!filtered.isEmpty()) return filtered;
        }
        return incomeDao.listByDateRange(userId, start, end);
    }
}
