package com.example.personalfinancialmanagement;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

class UserDaoSqlite implements UserDao {
    private final SqliteHelper helper;
    UserDaoSqlite(SqliteHelper h){ this.helper = h; }

    @Override public long insert(User user) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", user.username);
        cv.put("passwordHash", user.passwordHash);
        cv.put("fullName", user.fullName);
        cv.put("email", user.email);
        cv.put("phone", user.phone);
        return db.insert("users", null, cv);
    }

    @Override public User findByUsername(String username) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,username,passwordHash,fullName,email,phone FROM users WHERE username=? LIMIT 1", new String[]{username});
        User u = null; if (c.moveToFirst()) { u = new User(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)); }
        c.close(); return u;
    }

    @Override public User findById(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id,username,passwordHash,fullName,email,phone FROM users WHERE id=? LIMIT 1", new String[]{String.valueOf(id)});
        User u = null; if (c.moveToFirst()) { u = new User(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)); }
        c.close(); return u;
    }

    @Override public int update(User user) {
        SQLiteDatabase db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("username", user.username);
        cv.put("passwordHash", user.passwordHash);
        cv.put("fullName", user.fullName);
        cv.put("email", user.email);
        cv.put("phone", user.phone);
        return db.update("users", cv, "id=?", new String[]{String.valueOf(user.id)});
    }
}
