package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

class SavingsRepository {
    private final SavingsGoalDao goalDao;
    private final SavingsContributionDao contributionDao;
    private final SavingsMonthlyGoalDao monthlyGoalDao;

    SavingsRepository(Context ctx) {
        AppDatabase db = AppDatabase.getInstance(ctx);
        this.goalDao = db.savingsGoalDao();
        this.contributionDao = db.savingsContributionDao();
        this.monthlyGoalDao = db.savingsMonthlyGoalDao();
    }

    List<SavingsGoal> listGoals(long userId) {
        return goalDao.list(userId);
    }

    long addGoal(long userId, String title, double targetAmount, String iconKey) {
        return goalDao.insert(new SavingsGoal(userId, title, targetAmount, iconKey, System.currentTimeMillis()));
    }

    double goalProgress(long userId, long goalId) {
        return contributionDao.totalForGoal(userId, goalId);
    }

    long addContribution(long userId, Long goalId, double amount, long whenUtc) {
        return contributionDao.insert(new SavingsContribution(userId, goalId, amount, whenUtc));
    }

    double monthSavings(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        return contributionDao.sumForMonth(userId, start, end);
    }

    double monthAutoAllocated(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        return contributionDao.sumAutoForMonth(userId, start, end);
    }

    SavingsMonthlyGoal getOrCreateMonthlyGoal(long userId, int monthKey, double defaultTarget) {
        SavingsMonthlyGoal g = monthlyGoalDao.find(userId, monthKey);
        if (g == null) {
            g = new SavingsMonthlyGoal(userId, monthKey, defaultTarget);
            g.id = monthlyGoalDao.upsert(g);
        }
        return g;
    }

    void setMonthlyGoal(long userId, int monthKey, double target) {
        SavingsMonthlyGoal g = monthlyGoalDao.find(userId, monthKey);
        if (g == null) {
            monthlyGoalDao.upsert(new SavingsMonthlyGoal(userId, monthKey, target));
        } else {
            g.targetAmount = target;
            monthlyGoalDao.update(g);
        }
    }
}
