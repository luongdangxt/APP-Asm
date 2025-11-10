package com.example.personalfinancialmanagement;

import java.util.List;

interface NotificationDao {
    long insert(AppNotification notification);
    int update(AppNotification notification);
    List<AppNotification> list(long userId, String typeFilter, boolean onlyUnread);
    int markRead(long id, boolean isRead);
    int markAllRead(long userId);
    int delete(long id);
    int clear(long userId);
    long count(long userId, boolean onlyUnread);
}
