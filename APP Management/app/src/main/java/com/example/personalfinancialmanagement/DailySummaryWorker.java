package com.example.personalfinancialmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Generates a daily spending summary notification at 8 PM.
 */
public class DailySummaryWorker extends Worker {
    public static final String KEY_USER_ID = "key_user_id";
    static final int NOTIFICATION_ID = 5002;

    public DailySummaryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long userId = getInputData().getLong(KEY_USER_ID, -1);
        Context ctx = getApplicationContext();

        SettingsRepository settings = new SettingsRepository(ctx);
        if (!settings.notificationsEnabled()) {
            return Result.success();
        }

        double totalIncome = 0;
        double totalExpense = 0;
        long now = System.currentTimeMillis();

        if (userId > 0) {
            IncomeRepository incomeRepo = new IncomeRepository(ctx);
            ExpenseRepository expenseRepo = new ExpenseRepository(ctx);
            List<Income> incomes = incomeRepo.listForDay(userId, now);
            List<Expense> expenses = expenseRepo.listForDay(userId, now);
            for (Income income : incomes) totalIncome += income.amount;
            for (Expense expense : expenses) totalExpense += expense.amount;
        }

        double balance = totalIncome - totalExpense;
        Locale locale = Locale.getDefault();
        NumberFormat currency = NumberFormat.getCurrencyInstance(locale);
        String message = String.format(
                locale,
                "Thu hôm nay: %s\nChi hôm nay: %s\nCân đối: %s",
                currency.format(totalIncome),
                currency.format(totalExpense),
                currency.format(balance)
        );

        String title = "Tổng kết thu chi hằng ngày";
        NotificationHelper.notifyGeneric(ctx, NOTIFICATION_ID, title, message);

        if (userId > 0) {
            NotificationRepository repo = new NotificationRepository(ctx);
            AppNotification item = new AppNotification(userId, title, message, NotificationRepository.TYPE_REMINDER, System.currentTimeMillis());
            repo.save(item);
        }

        return Result.success();
    }
}
