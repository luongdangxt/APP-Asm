package com.example.personalfinancialmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

class NotificationHelper {
    static final String CHANNEL_ID = "app_updates_channel";
    private static final String CHANNEL_NAME = "Daily Updates & Alerts";
    private static final String CHANNEL_DESC = "Thông báo ngân sách và lời nhắc hằng ngày";

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(CHANNEL_DESC);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private static void notify(Context context, int id, String title, String message) {
        ensureChannel(context);
        if (!hasPostPermission(context)) return;
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(com.example.personalfinancialmanagement.R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build());
        } catch (SecurityException ignored) {
            // If permission is revoked between check and notify, skip gracefully.
        }
    }

    private static boolean hasPostPermission(Context context) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    static void notifyBudget(Context context, String title, String message, int id) {
        notify(context, id, title, message);
    }

    static void notifyGeneric(Context context, int id, String title, String message) {
        notify(context, id, title, message);
    }
}
