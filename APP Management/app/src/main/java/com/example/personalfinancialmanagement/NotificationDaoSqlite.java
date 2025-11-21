package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class NotificationDaoSqlite implements NotificationDao {
    private final SqliteHelper helper;

    NotificationDaoSqlite(SqliteHelper helper) {
        this.helper = helper;
    }

    @Override
    public long insert(AppNotification notification) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", notification.userId);
        cv.put("title", notification.title);
        cv.put("message", notification.message);
        cv.put("type", notification.type);
        cv.put("createdAtUtc", notification.createdAtUtc);
        cv.put("isRead", notification.isRead ? 1 : 0);
        cv.put("actionLabel", notification.actionLabel);
        cv.put("actionTarget", notification.actionTarget);
        return db.insert("notifications", null, cv);
    }

    @Override
    public int update(AppNotification notification) {
        if (notification.id <= 0) return 0;
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", notification.title);
        cv.put("message", notification.message);
        cv.put("type", notification.type);
        cv.put("createdAtUtc", notification.createdAtUtc);
        cv.put("isRead", notification.isRead ? 1 : 0);
        cv.put("actionLabel", notification.actionLabel);
        cv.put("actionTarget", notification.actionTarget);
        return db.update("notifications", cv, "id=?", new String[]{String.valueOf(notification.id)});
    }

    @Override
    public List<AppNotification> list(long userId, String typeFilter, boolean onlyUnread) {
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT id,userId,title,message,type,createdAtUtc,isRead,actionLabel,actionTarget FROM notifications WHERE userId=?");
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));
        if (typeFilter != null && !"all".equalsIgnoreCase(typeFilter)) {
            sql.append(" AND type=?");
            args.add(typeFilter);
        }
        if (onlyUnread) {
            sql.append(" AND isRead=0");
        }
        sql.append(" ORDER BY createdAtUtc DESC");
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        ArrayList<AppNotification> list = new ArrayList<>();
        while (c.moveToNext()) {
            AppNotification item = new AppNotification(
                    c.getLong(1),
                    c.getString(2),
                    c.getString(3),
                    c.getString(4),
                    c.getLong(5)
            );
            item.id = c.getLong(0);
            item.isRead = c.getInt(6) == 1;
            item.actionLabel = c.getString(7);
            item.actionTarget = c.getString(8);
            list.add(item);
        }
        c.close();
        return list;
    }

    @Override
    public int markRead(long id, boolean isRead) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("isRead", isRead ? 1 : 0);
        return db.update("notifications", cv, "id=?", new String[]{String.valueOf(id)});
    }

    @Override
    public int markAllRead(long userId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("isRead", 1);
        return db.update("notifications", cv, "userId=?", new String[]{String.valueOf(userId)});
    }

    @Override
    public int delete(long id) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("notifications", "id=?", new String[]{String.valueOf(id)});
    }

    @Override
    public int clear(long userId) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("notifications", "userId=?", new String[]{String.valueOf(userId)});
    }

    @Override
    public long count(long userId, boolean onlyUnread) {
        SQLiteDatabase db = helper.getReadableDatabase();
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM notifications WHERE userId=?");
        ArrayList<String> args = new ArrayList<>();
        args.add(String.valueOf(userId));
        if (onlyUnread) {
            sql.append(" AND isRead=0");
        }
        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        long count = 0;
        if (c.moveToFirst()) {
            count = c.getLong(0);
        }
        c.close();
        return count;
    }
}
