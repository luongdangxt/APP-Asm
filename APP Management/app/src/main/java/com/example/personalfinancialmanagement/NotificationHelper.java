package com.example.personalfinancialmanagement;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat.from(context).notify(id, builder.build());
    }

    static void notifyBudget(Context context, String title, String message, int id) {
        notify(context, id, title, message);
    }

    static void notifyGeneric(Context context, int id, String title, String message) {
        notify(context, id, title, message);
    }
}
