package com.example.personalfinancialmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.List;

public class GoalAutoContributionWorker extends Worker {
    public static final String KEY_USER_ID = "userId";

    public GoalAutoContributionWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override public Result doWork() {
        long userId = getInputData().getLong(KEY_USER_ID, -1);
        if (userId <= 0) return Result.success();

        Context ctx = getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(ctx);
        SavingsGoalDao goalDao = db.savingsGoalDao();
        SavingsContributionDao contribDao = db.savingsContributionDao();
        long now = System.currentTimeMillis();

        // fetch goals
        List<SavingsGoal> goals = goalDao.list(userId);
        for (SavingsGoal g : goals) {
            if (g.deadlineUtc <= 0 || g.cadence != 0) continue; // only daily for now
            // compute remaining
            double saved = contribDao.totalForGoal(userId, g.id);
            double remaining = Math.max(0, g.targetAmount - saved);
            if (remaining <= 0) continue;

            // days left (inclusive today if before end-of-day)
            Calendar todayEnd = Calendar.getInstance();
            todayEnd.setTimeInMillis(now);
            todayEnd.set(Calendar.HOUR_OF_DAY, 23);
            todayEnd.set(Calendar.MINUTE, 59);
            todayEnd.set(Calendar.SECOND, 59);
            todayEnd.set(Calendar.MILLISECOND, 999);

            long msLeft = Math.max(0, g.deadlineUtc - todayEnd.getTimeInMillis());
            int daysLeft = (int) Math.max(1, Math.ceil(msLeft / (1000.0 * 60 * 60 * 24)));

            double todayAmount = remaining / daysLeft;
            SavingsContribution c = new SavingsContribution(userId, g.id, todayAmount, now);
            c.isAuto = true;
            contribDao.insert(c);
        }

        return Result.success();
    }
}
