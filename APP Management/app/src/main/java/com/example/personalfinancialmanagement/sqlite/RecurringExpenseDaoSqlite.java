package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

class RecurringExpenseDaoSqlite implements RecurringExpenseDao {
    private final SqliteHelper helper;
    RecurringExpenseDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long insert(RecurringExpense item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("userId", item.userId);
        cv.put("description", item.description);
        cv.put("amount", item.amount);
        cv.put("category", item.category);
        cv.put("startDateUtc", item.startDateUtc);
        if (item.endDateUtc == null) cv.putNull("endDateUtc"); else cv.put("endDateUtc", item.endDateUtc);
        cv.put("dayOfMonth", item.dayOfMonth);
        return db.insert("recurring_expenses", null, cv);
    }

    @Override public int update(RecurringExpense item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("description", item.description);
        cv.put("amount", item.amount);
        cv.put("category", item.category);
        cv.put("startDateUtc", item.startDateUtc);
        if (item.endDateUtc == null) cv.putNull("endDateUtc"); else cv.put("endDateUtc", item.endDateUtc);
        cv.put("dayOfMonth", item.dayOfMonth);
        return db.update("recurring_expenses", cv, "id=?", new String[]{String.valueOf(item.id)});
    }

    @Override public int delete(RecurringExpense item) {
        SQLiteDatabase db = helper.getWritableDatabase();
        return db.delete("recurring_expenses", "id=?", new String[]{String.valueOf(item.id)});
    }

    @Override public List<RecurringExpense> list(long userId) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,userId,description,amount,category,startDateUtc,endDateUtc,dayOfMonth FROM recurring_expenses WHERE userId=?",
                new String[]{String.valueOf(userId)});
        ArrayList<RecurringExpense> list = new ArrayList<>();
        while (c.moveToNext()) {
            long id = c.getLong(0);
            long uid = c.getLong(1);
            String description = c.getString(2);
            double amount = c.getDouble(3);
            String category = c.getString(4);
            long start = c.getLong(5);
            Long end = c.isNull(6) ? null : c.getLong(6);
            int day = c.getInt(7);
            RecurringExpense r = new RecurringExpense(uid, description, amount, category, start, end, day);
            r.id = id;
            list.add(r);
        }
        c.close(); return list;
    }
}
