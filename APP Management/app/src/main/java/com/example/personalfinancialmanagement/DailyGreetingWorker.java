package com.example.personalfinancialmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Schedules a "good morning" notification every day at 6 AM.
 */
public class DailyGreetingWorker extends Worker {
    public static final String KEY_USER_ID = "key_user_id";
    static final int NOTIFICATION_ID = 5001;

    public DailyGreetingWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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

        String title = "Chúc ngày mới";
        String message = "Chúc bạn một ngày mới tràn đầy năng lượng và quản lý chi tiêu hiệu quả!";
        NotificationHelper.notifyGeneric(ctx, NOTIFICATION_ID, title, message);

        if (userId > 0) {
            NotificationRepository repo = new NotificationRepository(ctx);
            AppNotification item = new AppNotification(userId, title, message, NotificationRepository.TYPE_SYSTEM, System.currentTimeMillis());
            repo.save(item);
        }

        return Result.success();
    }
}
