package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class IncomeDaoSqlite implements IncomeDao {
    private final SqliteHelper helper;
    IncomeDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long insert(Income income) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", income.userId);
        cv.put("title", income.title);
        cv.put("dateUtc", income.dateUtc);
        cv.put("amount", income.amount);
        cv.put("category", income.category);
        return db.insert("incomes", null, cv);
    }

    @Override public List<Income> latest(long userId, int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,title,dateUtc,amount,category FROM incomes WHERE userId=? ORDER BY dateUtc DESC LIMIT ?",
                new String[]{String.valueOf(userId), String.valueOf(limit)});
        ArrayList<Income> list = new ArrayList<>();
        while (c.moveToNext()) {
            Income i = new Income(c.getLong(1), c.getString(2), c.getLong(3), c.getDouble(4), c.getString(5));
            i.id = c.getLong(0);
            list.add(i);
        }
        c.close();
        return list;
    }

    @Override public List<Income> listByDateRange(long userId, long start, long end) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,title,dateUtc,amount,category FROM incomes WHERE userId=? AND dateUtc BETWEEN ? AND ? ORDER BY dateUtc DESC",
                new String[]{String.valueOf(userId), String.valueOf(start), String.valueOf(end)});
        ArrayList<Income> list = new ArrayList<>();
        while (c.moveToNext()) {
            Income i = new Income(c.getLong(1), c.getString(2), c.getLong(3), c.getDouble(4), c.getString(5));
            i.id = c.getLong(0);
            list.add(i);
        }
        c.close();
        return list;
    }
}
