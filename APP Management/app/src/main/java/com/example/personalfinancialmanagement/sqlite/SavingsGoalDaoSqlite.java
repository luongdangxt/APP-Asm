package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class SavingsGoalDaoSqlite implements SavingsGoalDao {
    private final SqliteHelper helper;
    SavingsGoalDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long insert(SavingsGoal goal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", goal.userId);
        cv.put("title", goal.title);
        cv.put("targetAmount", goal.targetAmount);
        cv.put("iconKey", goal.iconKey);
        cv.put("createdAtUtc", goal.createdAtUtc);
        cv.put("deadlineUtc", goal.deadlineUtc);
        cv.put("cadence", goal.cadence);
        return db.insert("savings_goals", null, cv);
    }

    @Override public int update(SavingsGoal goal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", goal.title);
        cv.put("targetAmount", goal.targetAmount);
        cv.put("iconKey", goal.iconKey);
        cv.put("deadlineUtc", goal.deadlineUtc);
        cv.put("cadence", goal.cadence);
        return db.update("savings_goals", cv, "id=?", new String[]{String.valueOf(goal.id)});
    }

    @Override public int delete(SavingsGoal goal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("savings_goals", "id=?", new String[]{String.valueOf(goal.id)});
    }

    @Override public List<SavingsGoal> list(long userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,title,targetAmount,iconKey,createdAtUtc,deadlineUtc,cadence FROM savings_goals WHERE userId=? ORDER BY createdAtUtc DESC",
                new String[]{String.valueOf(userId)});
        ArrayList<SavingsGoal> list = new ArrayList<>();
        while (c.moveToNext()) {
            SavingsGoal g = new SavingsGoal(c.getLong(1), c.getString(2), c.getDouble(3), c.getString(4), c.getLong(5));
            g.id = c.getLong(0); g.deadlineUtc = c.getLong(6); g.cadence = c.getInt(7);
            list.add(g);
        }
        c.close();
        return list;
    }

    @Override public SavingsGoal findById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,title,targetAmount,iconKey,createdAtUtc,deadlineUtc,cadence FROM savings_goals WHERE id=? LIMIT 1",
                new String[]{String.valueOf(id)});
        SavingsGoal g = null; if (c.moveToFirst()) { g = new SavingsGoal(c.getLong(1), c.getString(2), c.getDouble(3), c.getString(4), c.getLong(5)); g.id = c.getLong(0); g.deadlineUtc = c.getLong(6); g.cadence = c.getInt(7);} c.close(); return g;
    }
}
