package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class FeedbackDaoSqlite implements FeedbackDao {
    private final SqliteHelper helper;
    FeedbackDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long insert(Feedback f) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", f.userId);
        cv.put("message", f.message);
        cv.put("createdAt", f.createdAt);
        return db.insert("feedback", null, cv);
    }

    @Override public List<Feedback> list(long userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,message,createdAt FROM feedback WHERE userId=? ORDER BY createdAt DESC",
                new String[]{String.valueOf(userId)});
        ArrayList<Feedback> list = new ArrayList<>();
        while (c.moveToNext()) {
            Feedback f = new Feedback(c.getLong(1), c.getString(2), c.getLong(3));
            f.id = c.getLong(0); list.add(f);
        }
        c.close(); return list;
    }
}
