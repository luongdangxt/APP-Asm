package com.example.personalfinancialmanagement;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class SqliteHelper extends SQLiteOpenHelper {
    static final String DB_NAME = "campus_expense_db";
    // Set version >= previous Room version (8) to avoid downgrade issues.
    // If an older/larger version is encountered, we handle in onUpgrade/onDowngrade.
    static final int DB_VERSION = 10;

    SqliteHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, passwordHash TEXT NOT NULL, fullName TEXT, email TEXT, phone TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, description TEXT NOT NULL, " +
                "dateUtc INTEGER NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_user ON expenses(userId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_cat ON expenses(category)");

        db.execSQL("CREATE TABLE IF NOT EXISTS incomes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, title TEXT NOT NULL, " +
                "dateUtc INTEGER NOT NULL, amount REAL NOT NULL, category TEXT NOT NULL, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS budgets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, monthKey INTEGER NOT NULL, category TEXT NOT NULL, limitAmount REAL NOT NULL, " +
                "UNIQUE(userId, monthKey, category) ON CONFLICT REPLACE, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS recurring_expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER NOT NULL, " +
                "description TEXT NOT NULL, " +
                "amount REAL, " +
                "category TEXT NOT NULL, " +
                "startDateUtc INTEGER, " +
                "endDateUtc INTEGER, " +
                "dayOfMonth INTEGER NOT NULL, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS feedback (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, message TEXT, createdAt INTEGER, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS savings_goals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, title TEXT NOT NULL, targetAmount REAL NOT NULL, iconKey TEXT NOT NULL, createdAtUtc INTEGER, deadlineUtc INTEGER, cadence INTEGER, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS savings_contributions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, goalId INTEGER, amount REAL NOT NULL, dateUtc INTEGER NOT NULL, isAuto INTEGER NOT NULL DEFAULT 0, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(goalId) REFERENCES savings_goals(id) ON DELETE SET NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS savings_monthly_goals (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, userId INTEGER NOT NULL, monthKey INTEGER NOT NULL, targetAmount REAL NOT NULL, " +
                "UNIQUE(userId, monthKey) ON CONFLICT REPLACE, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");

        db.execSQL("CREATE TABLE IF NOT EXISTS notifications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER NOT NULL, " +
                "title TEXT NOT NULL, " +
                "message TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "createdAtUtc INTEGER NOT NULL, " +
                "isRead INTEGER NOT NULL DEFAULT 0, " +
                "actionLabel TEXT, " +
                "actionTarget TEXT, " +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(userId, createdAtUtc DESC)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(userId, isRead)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now, recreate tables on upgrade
        db.execSQL("DROP TABLE IF EXISTS notifications");
        db.execSQL("DROP TABLE IF EXISTS savings_monthly_goals");
        db.execSQL("DROP TABLE IF EXISTS savings_contributions");
        db.execSQL("DROP TABLE IF EXISTS savings_goals");
        db.execSQL("DROP TABLE IF EXISTS feedback");
        db.execSQL("DROP TABLE IF EXISTS recurring_expenses");
        db.execSQL("DROP TABLE IF EXISTS budgets");
        db.execSQL("DROP TABLE IF EXISTS incomes");
        db.execSQL("DROP TABLE IF EXISTS expenses");
        db.execSQL("DROP TABLE IF EXISTS users");
        onCreate(db);
    }

    @Override public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // When moving from previous Room schema (v<=8) to this helper (v=9), treat as destructive migration
        onUpgrade(db, oldVersion, newVersion);
    }
}
