package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;
import java.util.ArrayList;

import com.example.personalfinancialmanagement.network.FinanceRemoteRepository;

public class SavingsRepository {
    private final SavingsGoalDao goalDao;
    private final SavingsContributionDao contributionDao;
    private final SavingsMonthlyGoalDao monthlyGoalDao;
    private final FinanceRemoteRepository remote;

    public SavingsRepository(Context ctx) {
        AppDatabase db = AppDatabase.getInstance(ctx);
        this.goalDao = db.savingsGoalDao();
        this.contributionDao = db.savingsContributionDao();
        this.monthlyGoalDao = db.savingsMonthlyGoalDao();
        this.remote = new FinanceRemoteRepository(ctx);
    }

    public List<SavingsGoal> listGoals(long userId) {
        List<SavingsGoal> remoteList = remote.listSavingsGoals(userId);
        if (remoteList != null && !remoteList.isEmpty()) return remoteList;
        return goalDao.list(userId);
    }

    public long addGoal(long userId, String title, double targetAmount, String iconKey) {
        boolean ok = remote.addSavingsGoal(userId, title, targetAmount, iconKey);
        long id = goalDao.insert(new SavingsGoal(userId, title, targetAmount, iconKey, System.currentTimeMillis()));
        return ok ? id : id;
    }

    public double goalProgress(long userId, long goalId) {
        List<SavingsContribution> list = remote.listSavingsContributions(userId, goalId);
        if (list != null && !list.isEmpty()) {
            double sum = 0;
            for (SavingsContribution c : list) sum += c.amount;
            return sum;
        }
        return contributionDao.totalForGoal(userId, goalId);
    }

    public long addContribution(long userId, Long goalId, double amount, long whenUtc) {
        boolean ok = remote.addSavingsContribution(userId, goalId, amount, whenUtc, false);
        return contributionDao.insert(new SavingsContribution(userId, goalId, amount, whenUtc));
    }

    public double monthSavings(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        List<SavingsContribution> list = remote.listSavingsContributions(userId, null);
        if (list != null && !list.isEmpty()) {
            double sum = 0;
            for (SavingsContribution c : list) if (c.dateUtc >= start && c.dateUtc <= end) sum += c.amount;
            return sum;
        }
        return contributionDao.sumForMonth(userId, start, end);
    }

    public double monthAutoAllocated(long userId, long now) {
        long start = MonthUtils.monthStartUtcMillis(now);
        long end = MonthUtils.monthEndUtcMillis(now);
        List<SavingsContribution> list = remote.listSavingsContributions(userId, null);
        if (list != null && !list.isEmpty()) {
            double sum = 0;
            for (SavingsContribution c : list) if (c.isAuto && c.dateUtc >= start && c.dateUtc <= end) sum += c.amount;
            return sum;
        }
        return contributionDao.sumAutoForMonth(userId, start, end);
    }

    public SavingsMonthlyGoal getOrCreateMonthlyGoal(long userId, int monthKey, double defaultTarget) {
        SavingsMonthlyGoal g = remote.getOrCreateMonthlyGoal(userId, monthKey, defaultTarget);
        if (g != null) return g;
        SavingsMonthlyGoal local = monthlyGoalDao.find(userId, monthKey);
        if (local == null) {
            local = new SavingsMonthlyGoal(userId, monthKey, defaultTarget);
            local.id = monthlyGoalDao.upsert(local);
        }
        return local;
    }

    public void setMonthlyGoal(long userId, int monthKey, double target) {
        boolean ok = remote.setMonthlyGoal(userId, monthKey, target);
        SavingsMonthlyGoal g = monthlyGoalDao.find(userId, monthKey);
        if (g == null) {
            monthlyGoalDao.upsert(new SavingsMonthlyGoal(userId, monthKey, target));
        } else {
            g.targetAmount = target;
            monthlyGoalDao.update(g);
        }
    }

    public void deleteGoal(long userId, SavingsGoal goal) {
        if (goal == null) return;
        remote.deleteSavingsGoal(userId, goal.title);
        goalDao.delete(goal);
        // Do not remove contributions; they stay as transaction history.
    }
}
