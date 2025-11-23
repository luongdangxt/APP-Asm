package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
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

}
