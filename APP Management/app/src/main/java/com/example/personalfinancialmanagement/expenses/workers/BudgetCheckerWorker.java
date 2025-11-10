package com.example.personalfinancialmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class BudgetCheckerWorker extends Worker {
    public static final String KEY_USER_ID = "key_user_id";

    public BudgetCheckerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long userId = getInputData().getLong(KEY_USER_ID, -1);
        if (userId <= 0) return Result.success();

        Context ctx = getApplicationContext();
        SettingsRepository settings = new SettingsRepository(ctx);
        if (!settings.notificationsEnabled() || !settings.budgetAlertsEnabled()) {
            return Result.success();
        }
        ExpenseRepository expenseRepo = new ExpenseRepository(ctx);
        BudgetRepository budgetRepo = new BudgetRepository(ctx);

        int monthKey = MonthUtils.monthKey(System.currentTimeMillis());
        double totalByCat;
        List<Budget> budgets = budgetRepo.listForMonth(userId, monthKey);
        for (Budget b : budgets) {
            totalByCat = 0;
            for (Expense e : expenseRepo.listForCurrentMonth(userId, System.currentTimeMillis())) {
                if (b.category.equals(e.category)) totalByCat += e.amount;
            }
            if (b.limitAmount > 0) {
                double ratio = totalByCat / b.limitAmount;
                if (ratio >= 1.0) {
                    NotificationHelper.notifyBudget(ctx, "Budget exceeded", b.category + " over limit", (int) (b.id % Integer.MAX_VALUE));
                } else if (ratio >= 0.8) {
                    NotificationHelper.notifyBudget(ctx, "Budget warning", b.category + " reached 80%", (int) ((b.id + 1000) % Integer.MAX_VALUE));
                }
            }
        }
        return Result.success();
    }
}
