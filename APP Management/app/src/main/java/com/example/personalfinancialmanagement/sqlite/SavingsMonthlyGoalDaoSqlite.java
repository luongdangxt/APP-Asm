package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class SavingsMonthlyGoalDaoSqlite implements SavingsMonthlyGoalDao {
    private final SqliteHelper helper;
    SavingsMonthlyGoalDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long upsert(SavingsMonthlyGoal goal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", goal.userId);
        cv.put("monthKey", goal.monthKey);
        cv.put("targetAmount", goal.targetAmount);
        return db.insertWithOnConflict("savings_monthly_goals", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override public int update(SavingsMonthlyGoal goal) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("targetAmount", goal.targetAmount);
        return db.update("savings_monthly_goals", cv, "id=?", new String[]{String.valueOf(goal.id)});
    }

    @Override public SavingsMonthlyGoal find(long userId, int monthKey) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,monthKey,targetAmount FROM savings_monthly_goals WHERE userId=? AND monthKey=? LIMIT 1",
                new String[]{String.valueOf(userId), String.valueOf(monthKey)});
        SavingsMonthlyGoal g = null; if (c.moveToFirst()) { g = new SavingsMonthlyGoal(c.getLong(1), c.getInt(2), c.getDouble(3)); g.id = c.getLong(0);} c.close(); return g;
    }
}
