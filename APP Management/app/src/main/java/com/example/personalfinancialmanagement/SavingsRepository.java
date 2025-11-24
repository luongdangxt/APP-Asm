package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;
import java.util.ArrayList;

import com.example.personalfinancialmanagement.network.FinanceRemoteRepository;

public class SavingsRepository {
    private final FinanceRemoteRepository remote;

    public SavingsRepository(Context ctx) {
        this.remote = new FinanceRemoteRepository(ctx);
    }

    public List<SavingsGoal> listGoals(long userId) {
        List<SavingsGoal> remoteList = remote.listSavingsGoals(userId);
        if (remoteList != null) return remoteList;
        return java.util.Collections.emptyList();
    }

    public boolean addGoal(long userId, String title, double targetAmount, String iconKey, Long deadlineUtc, Integer cadence) {
        return remote.addSavingsGoal(userId, title, targetAmount, iconKey, deadlineUtc, cadence);
    }

    public double goalProgress(long userId, long goalId) {
        List<SavingsContribution> list = remote.listSavingsContributions(userId, goalId);
        if (list != null) {
            double sum = 0;
            for (SavingsContribution c : list) sum += c.amount;
            return sum;
        }
        return 0;
    }

    public boolean addContribution(long userId, Long goalId, double amount, long whenUtc) {
        return remote.addSavingsContribution(userId, goalId, amount, whenUtc, false);
    }

    public double monthSavings(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        List<SavingsContribution> list = remote.listSavingsContributions(userId, null);
        if (list != null) {
            double sum = 0;
            for (SavingsContribution c : list) if (c.dateUtc >= start && c.dateUtc <= end) sum += c.amount;
            return sum;
        }
        return 0;
    }

    public double monthAutoAllocated(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        List<SavingsContribution> list = remote.listSavingsContributions(userId, null);
        if (list != null) {
            double sum = 0;
            for (SavingsContribution c : list) if (c.isAuto && c.dateUtc >= start && c.dateUtc <= end) sum += c.amount;
            return sum;
        }
        return 0;
    }

    public SavingsMonthlyGoal getOrCreateMonthlyGoal(long userId, int monthKey, double defaultTarget) {
        SavingsMonthlyGoal g = remote.getOrCreateMonthlyGoal(userId, monthKey, defaultTarget);
        if (g != null) return g;
        return new SavingsMonthlyGoal(userId, monthKey, defaultTarget);
    }

    public boolean setMonthlyGoal(long userId, int monthKey, double target) {
        return remote.setMonthlyGoal(userId, monthKey, target);
    }

    public void deleteGoal(long userId, SavingsGoal goal) {
        if (goal == null) return;
        remote.deleteSavingsGoal(userId, goal.title);
    }

    public boolean deleteGoalByTitle(long userId, String title) {
        if (title == null || title.trim().isEmpty()) return false;
        return remote.deleteSavingsGoal(userId, title);
    }
}
