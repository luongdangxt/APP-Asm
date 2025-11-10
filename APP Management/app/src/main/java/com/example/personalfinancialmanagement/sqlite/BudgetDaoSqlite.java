package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class BudgetDaoSqlite implements BudgetDao {
    private final SqliteHelper helper;
    BudgetDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long upsert(Budget budget) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", budget.userId);
        cv.put("monthKey", budget.monthKey);
        cv.put("category", budget.category);
        cv.put("limitAmount", budget.limitAmount);
        return db.insertWithOnConflict("budgets", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override public int update(Budget budget) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("limitAmount", budget.limitAmount);
        return db.update("budgets", cv, "id=?", new String[]{String.valueOf(budget.id)});
    }

    @Override public List<Budget> listForMonth(long userId, int monthKey) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,monthKey,category,limitAmount FROM budgets WHERE userId=? AND monthKey=?",
                new String[]{String.valueOf(userId), String.valueOf(monthKey)});
        ArrayList<Budget> list = new ArrayList<>();
        while (c.moveToNext()) {
            Budget b = new Budget(c.getLong(1), c.getInt(2), c.getString(3), c.getDouble(4));
            b.id = c.getLong(0);
            list.add(b);
        }
        c.close();
        return list;
    }

    @Override public Budget find(long userId, int monthKey, String category) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,monthKey,category,limitAmount FROM budgets WHERE userId=? AND monthKey=? AND category=? LIMIT 1",
                new String[]{String.valueOf(userId), String.valueOf(monthKey), category});
        Budget b = null; if (c.moveToFirst()) { b = new Budget(c.getLong(1), c.getInt(2), c.getString(3), c.getDouble(4)); b.id = c.getLong(0);} c.close(); return b;
    }
}
