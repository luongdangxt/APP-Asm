package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class ExpenseDaoSqlite implements ExpenseDao {
    private final SqliteHelper helper;
    ExpenseDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long insert(Expense expense) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", expense.userId);
        cv.put("description", expense.description);
        cv.put("dateUtc", expense.dateUtc);
        cv.put("amount", expense.amount);
        cv.put("category", expense.category);
        return db.insert("expenses", null, cv);
    }

    @Override public int update(Expense expense) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("description", expense.description);
        cv.put("dateUtc", expense.dateUtc);
        cv.put("amount", expense.amount);
        cv.put("category", expense.category);
        return db.update("expenses", cv, "id=?", new String[]{String.valueOf(expense.id)});
    }

    @Override public int delete(Expense expense) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("expenses", "id=?", new String[]{String.valueOf(expense.id)});
    }

    @Override public List<Expense> listByDateRange(long userId, long start, long end) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,description,dateUtc,amount,category FROM expenses WHERE userId=? AND dateUtc BETWEEN ? AND ? ORDER BY dateUtc DESC",
                new String[]{String.valueOf(userId), String.valueOf(start), String.valueOf(end)});
        ArrayList<Expense> list = new ArrayList<>();
        while (c.moveToNext()) {
            Expense e = new Expense(c.getLong(1), c.getString(2), c.getLong(3), c.getDouble(4), c.getString(5));
            e.id = c.getLong(0);
            list.add(e);
        }
        c.close();
        return list;
    }

    @Override public double sumByCategoryForMonth(long userId, String category, String monthKeyStr) {
        // monthKeyStr format: YYYYMM
        SQLiteDatabase db = helper.getReadableDatabase();
        long start; long end;
        try {
            int year = Integer.parseInt(monthKeyStr.substring(0,4));
            int month = Integer.parseInt(monthKeyStr.substring(4,6)) - 1;
            java.util.Calendar c = java.util.Calendar.getInstance();
            c.set(java.util.Calendar.YEAR, year); c.set(java.util.Calendar.MONTH, month); c.set(java.util.Calendar.DAY_OF_MONTH,1);
            c.set(java.util.Calendar.HOUR_OF_DAY,0); c.set(java.util.Calendar.MINUTE,0); c.set(java.util.Calendar.SECOND,0); c.set(java.util.Calendar.MILLISECOND,0);
            start = c.getTimeInMillis();
            c.set(java.util.Calendar.DAY_OF_MONTH, c.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
            c.set(java.util.Calendar.HOUR_OF_DAY,23); c.set(java.util.Calendar.MINUTE,59); c.set(java.util.Calendar.SECOND,59); c.set(java.util.Calendar.MILLISECOND,999);
            end = c.getTimeInMillis();
        } catch (Exception e) { start = 0; end = Long.MAX_VALUE; }
        Cursor c = db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE userId=? AND category=? AND dateUtc BETWEEN ? AND ?",
                new String[]{String.valueOf(userId), category, String.valueOf(start), String.valueOf(end)});
        double sum = 0; if (c.moveToFirst()) sum = c.getDouble(0); c.close(); return sum;
    }

    @Override public List<Expense> latest(long userId, int limit) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,description,dateUtc,amount,category FROM expenses WHERE userId=? ORDER BY dateUtc DESC LIMIT ?",
                new String[]{String.valueOf(userId), String.valueOf(limit)});
        ArrayList<Expense> list = new ArrayList<>();
        while (c.moveToNext()) {
            Expense e = new Expense(c.getLong(1), c.getString(2), c.getLong(3), c.getDouble(4), c.getString(5));
            e.id = c.getLong(0);
            list.add(e);
        }
        c.close();
        return list;
    }
}
