package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

class NotificationRepository {
    static final String TYPE_BUDGET = "budget";
    static final String TYPE_GOAL = "goal";
    static final String TYPE_REMINDER = "reminder";
    static final String TYPE_SYSTEM = "system";

    private final NotificationDao dao;

    NotificationRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).notificationDao();
    }

    public List<AppNotification> list(long userId, String type, boolean onlyUnread) {
        if (userId <= 0) return new ArrayList<>();
        return dao.list(userId, type, onlyUnread);
    }

    public long unreadCount(long userId) {
        if (userId <= 0) return 0;
        return dao.count(userId, true);
    }

    public long totalCount(long userId) {
        if (userId <= 0) return 0;
        return dao.count(userId, false);
    }

    public void markRead(long id, boolean isRead) {
        dao.markRead(id, isRead);
    }

    public void markAllRead(long userId) {
        if (userId <= 0) return;
        dao.markAllRead(userId);
    }

    public void delete(long id) {
        dao.delete(id);
    }

    public void clear(long userId) {
        if (userId <= 0) return;
        dao.clear(userId);
    }

    public AppNotification save(AppNotification notification) {
        if (notification.id > 0) {
            dao.update(notification);
            return notification;
        }
        long id = dao.insert(notification);
        notification.id = id;
        return notification;
    }

    /**
     * Seed the inbox with a few sample notifications so the screen does not look empty at first.
     */
    public void ensureSeeded(long userId) {
        if (userId <= 0) return;
        if (dao.count(userId, false) > 0) return;
        List<AppNotification> samples = buildSampleNotifications(userId);
        for (AppNotification notification : samples) {
            save(notification);
        }
    }

    private List<AppNotification> buildSampleNotifications(long userId) {
        long now = System.currentTimeMillis();
        ArrayList<AppNotification> list = new ArrayList<>();

        AppNotification budget = new AppNotification(
                userId,
                "Budget Alert: Dining",
                "You have used 82% of your Dining budget this month. Review your expenses to stay on track.",
                TYPE_BUDGET,
                now - hoursToMillis(5)
        );
        budget.actionLabel = "View budget";
        budget.actionTarget = "budget_overview";
        list.add(budget);

        AppNotification goal = new AppNotification(
                userId,
                "Savings Goal Reached",
                "Great job! You contributed $120 to your “New Bike” goal this week.",
                TYPE_GOAL,
                now - hoursToMillis(26)
        );
        goal.actionLabel = "View goals";
        goal.actionTarget = "goals";
        list.add(goal);

        AppNotification reminder = new AppNotification(
                userId,
                "Upcoming Payment",
                "Your recurring rent payment is scheduled for " + formatRelativeDate(now + hoursToMillis(48)) + ".",
                TYPE_REMINDER,
                now - hoursToMillis(52)
        );
        reminder.actionLabel = "View schedule";
        reminder.actionTarget = "recurring";
        list.add(reminder);

        AppNotification system = new AppNotification(
                userId,
                "New dark theme applied",
                "We’ve refreshed your dashboard with a gradient background. Let us know what you think!",
                TYPE_SYSTEM,
                now - hoursToMillis(78)
        );
        system.isRead = true;
        system.actionLabel = "Share feedback";
        system.actionTarget = "feedback";
        list.add(system);

        return list;
    }

    private static long hoursToMillis(int hours) {
        return hours * 60L * 60L * 1000L;
    }

    private static String formatRelativeDate(long timeMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeMillis);
        return String.format(Locale.getDefault(), "%1$tb %1$te", cal);
    }
}
