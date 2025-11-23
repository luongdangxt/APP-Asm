package com.example.personalfinancialmanagement;

import android.content.Context;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class BudgetAlertManager {
    static void onExpenseLogged(Context context, long userId, String category, long whenUtc) {
        if (context == null || userId <= 0 || category == null) return;
        checkAndNotify(context.getApplicationContext(), userId, category, MonthUtils.monthKey(whenUtc));
    }

    public static void reEvaluate(Context context, long userId, String category, int monthKey) {
        if (context == null || userId <= 0 || category == null) return;
        checkAndNotify(context.getApplicationContext(), userId, category, monthKey);
    }

    private static void checkAndNotify(Context ctx, long userId, String rawCategory, int monthKey) {
        String category = CategoryPreferences.sanitizeLabel(rawCategory);
        if (category.isEmpty()) return;

        BudgetRepository budgetRepo = new BudgetRepository(ctx);
        Budget budget = budgetRepo.find(userId, monthKey, category);
        if (budget == null || budget.limitAmount <= 0) return;

        ExpenseRepository expenseRepo = new ExpenseRepository(ctx);
        double spent = expenseRepo.sumForCategory(userId, category, monthKey);
        double percent = (spent / budget.limitAmount) * 100d;
        int threshold = highestThreshold(percent);
        if (threshold <= 0) return;

        BudgetAlertTracker tracker = new BudgetAlertTracker(ctx);
        int last = tracker.getLastThreshold(userId, monthKey, category);
        if (threshold <= last) return;

        tracker.setLastThreshold(userId, monthKey, category, threshold);

        SettingsRepository settings = new SettingsRepository(ctx);
        if (!settings.notificationsEnabled() || !settings.budgetAlertsEnabled()) {
            return;
        }

        String title = buildTitle(threshold, category);
        String message = buildMessage(threshold, category, spent, budget.limitAmount);
        int notificationId = Math.abs(Objects.hash(userId, monthKey, category));
        NotificationHelper.notifyBudget(ctx, title, message, notificationId);

        NotificationRepository repo = new NotificationRepository(ctx);
        AppNotification log = new AppNotification(userId, title, message, NotificationRepository.TYPE_BUDGET, System.currentTimeMillis());
        log.actionLabel = "Manage budgets";
        log.actionTarget = "budget_overview";
        repo.save(log);
    }

    private static int highestThreshold(double percent) {
        int pct = (int) Math.floor(percent);
        if (pct < 50) return 0;
        int highest = 0;
        if (pct >= 50) highest = 50;
        if (pct >= 80) highest = 80;
        if (pct >= 100) {
            highest = 100;
            if (pct > 100) {
                int extra = ((pct - 100) / 10) * 10;
                highest = 100 + extra;
            }
        }
        return highest;
    }

    private static String buildTitle(int threshold, String category) {
        if (threshold <= 0) return "Budget update";
        if (threshold < 100) {
            return String.format(Locale.getDefault(), "%s budget %d%% used", category, threshold);
        } else if (threshold == 100) {
            return String.format(Locale.getDefault(), "%s budget reached", category);
        } else {
            return String.format(Locale.getDefault(), "%s budget %d%% used", category, threshold);
        }
    }

    private static String buildMessage(int threshold, String category, double spent, double limit) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.getDefault());
        if (threshold < 100) {
            return String.format(Locale.getDefault(),
                    "%s spending is now %d%% of the monthly budget (%s of %s).",
                    category,
                    threshold,
                    currency.format(spent),
                    currency.format(limit));
        } else if (threshold == 100) {
            return String.format(Locale.getDefault(),
                    "%s budget is fully used (%s of %s). Spend carefully for the rest of the month.",
                    category,
                    currency.format(spent),
                    currency.format(limit));
        } else {
            return String.format(Locale.getDefault(),
                    "%s spending is %d%% of the budget (%s of %s).",
                    category,
                    threshold,
                    currency.format(spent),
                    currency.format(limit));
        }
    }
}
