package com.example.personalfinancialmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.List;

public class RecurringApplierWorker extends Worker {
    public static final String KEY_USER_ID = "key_user_id";

    public RecurringApplierWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        long userId = getInputData().getLong(KEY_USER_ID, -1);
        if (userId <= 0) return Result.success();
        Context ctx = getApplicationContext();
        RecurringRepository repo = new RecurringRepository(ctx);
        ExpenseRepository expenseRepo = new ExpenseRepository(ctx);

        Calendar c = Calendar.getInstance();
        int today = c.get(Calendar.DAY_OF_MONTH);
        List<RecurringExpense> items = repo.list(userId);
        for (RecurringExpense r : items) {
            if (r.endDateUtc != null && r.endDateUtc < System.currentTimeMillis()) continue;
            if (today == r.dayOfMonth) {
                expenseRepo.add(new Expense(userId, r.description, System.currentTimeMillis(), r.amount, r.category));
            }
        }
        return Result.success();
    }
}
