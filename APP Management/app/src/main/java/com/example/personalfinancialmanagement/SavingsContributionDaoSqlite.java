package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class SavingsContributionDaoSqlite implements SavingsContributionDao {
    private final SqliteHelper helper;
    SavingsContributionDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long insert(SavingsContribution c) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", c.userId);
        if (c.goalId == null) cv.putNull("goalId"); else cv.put("goalId", c.goalId);
        cv.put("amount", c.amount);
        cv.put("dateUtc", c.dateUtc);
        cv.put("isAuto", c.isAuto ? 1 : 0);
        return db.insert("savings_contributions", null, cv);
    }

    @Override public double sumForMonth(long userId, long start, long end) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM savings_contributions WHERE userId=? AND dateUtc BETWEEN ? AND ?",
                new String[]{String.valueOf(userId), String.valueOf(start), String.valueOf(end)});
        double sum = 0; if (c.moveToFirst()) sum = c.getDouble(0); c.close(); return sum;
    }

    @Override public double totalForGoal(long userId, long goalId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM savings_contributions WHERE userId=? AND goalId=?",
                new String[]{String.valueOf(userId), String.valueOf(goalId)});
        double sum = 0; if (c.moveToFirst()) sum = c.getDouble(0); c.close(); return sum;
    }

    @Override public List<SavingsContribution> listForGoal(long userId, long goalId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,goalId,amount,dateUtc,isAuto FROM savings_contributions WHERE userId=? AND goalId=? ORDER BY dateUtc DESC",
                new String[]{String.valueOf(userId), String.valueOf(goalId)});
        ArrayList<SavingsContribution> list = new ArrayList<>();
        while (c.moveToNext()) {
            SavingsContribution sc = new SavingsContribution(c.getLong(1), c.isNull(2) ? null : c.getLong(2), c.getDouble(3), c.getLong(4));
            sc.id = c.getLong(0); sc.isAuto = c.getInt(5) == 1; list.add(sc);
        }
        c.close(); return list;
    }

    @Override public double sumAutoForMonth(long userId, long start, long end) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM savings_contributions WHERE userId=? AND isAuto=1 AND dateUtc BETWEEN ? AND ?",
                new String[]{String.valueOf(userId), String.valueOf(start), String.valueOf(end)});
        double sum = 0; if (c.moveToFirst()) sum = c.getDouble(0); c.close(); return sum;
    }
}
