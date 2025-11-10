package com.example.personalfinancialmanagement;

import android.content.Context;

/**
 * Lightweight SQLite provider that exposes DAO-like interfaces backed by SQLiteOpenHelper.
 * Keeps the existing API surface so the rest of the app does not need to change.
 */
public class AppDatabase {
    private static volatile AppDatabase INSTANCE;
    private final SqliteHelper helper;

    private final ExpenseDao expenseDao;
    private final IncomeDao incomeDao;
    private final BudgetDao budgetDao;
    private final RecurringExpenseDao recurringExpenseDao;
    private final FeedbackDao feedbackDao;
    private final UserDao userDao;
    private final SavingsGoalDao savingsGoalDao;
    private final SavingsContributionDao savingsContributionDao;
    private final SavingsMonthlyGoalDao savingsMonthlyGoalDao;
    private final NotificationDao notificationDao;

    private AppDatabase(Context ctx) {
        this.helper = new SqliteHelper(ctx.getApplicationContext());
        this.expenseDao = new ExpenseDaoSqlite(helper);
        this.incomeDao = new IncomeDaoSqlite(helper);
        this.budgetDao = new BudgetDaoSqlite(helper);
        this.recurringExpenseDao = new RecurringExpenseDaoSqlite(helper);
        this.feedbackDao = new FeedbackDaoSqlite(helper);
        this.userDao = new UserDaoSqlite(helper);
        this.savingsGoalDao = new SavingsGoalDaoSqlite(helper);
        this.savingsContributionDao = new SavingsContributionDaoSqlite(helper);
        this.savingsMonthlyGoalDao = new SavingsMonthlyGoalDaoSqlite(helper);
        this.notificationDao = new NotificationDaoSqlite(helper);
    }

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) INSTANCE = new AppDatabase(context);
            }
        }
        return INSTANCE;
    }

    public static void reset(Context context) {
        synchronized (AppDatabase.class) { INSTANCE = null; }
    }

    public ExpenseDao expenseDao() { return expenseDao; }
    public IncomeDao incomeDao() { return incomeDao; }
    public BudgetDao budgetDao() { return budgetDao; }
    public RecurringExpenseDao recurringExpenseDao() { return recurringExpenseDao; }
    public FeedbackDao feedbackDao() { return feedbackDao; }
    public UserDao userDao() { return userDao; }
    public SavingsGoalDao savingsGoalDao() { return savingsGoalDao; }
    public SavingsContributionDao savingsContributionDao() { return savingsContributionDao; }
    public SavingsMonthlyGoalDao savingsMonthlyGoalDao() { return savingsMonthlyGoalDao; }
    public NotificationDao notificationDao() { return notificationDao; }

    // Clear only data that belongs to a specific user. Does not remove the user row itself.
    public void clearUserData(long userId) {
        if (userId <= 0) return;
        android.database.sqlite.SQLiteDatabase db = helper.getWritableDatabase();
        db.beginTransaction();
        try {
            String[] args = new String[]{String.valueOf(userId)};
            db.delete("expenses", "userId=?", args);
            db.delete("incomes", "userId=?", args);
            db.delete("budgets", "userId=?", args);
            db.delete("recurring_expenses", "userId=?", args);
            db.delete("feedback", "userId=?", args);
            db.delete("savings_contributions", "userId=?", args);
            db.delete("savings_goals", "userId=?", args);
            db.delete("savings_monthly_goals", "userId=?", args);
            db.delete("notifications", "userId=?", args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
