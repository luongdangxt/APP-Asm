package com.example.personalfinancialmanagement;

/**
 * Represents an in-app notification that can be displayed inside the inbox screen.
 */
public class AppNotification {
    public long id;
    public long userId;
    public String title;
    public String message;
    public String type;
    public long createdAtUtc;
    public boolean isRead;
    public String actionLabel;
    public String actionTarget;

    public AppNotification(long userId, String title, String message, String type, long createdAtUtc) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAtUtc = createdAtUtc;
    }
}
