package com.example.personalfinancialmanagement;

import android.content.Context;

import java.util.List;

class RecurringRepository {
    private final RecurringExpenseDao dao;
    RecurringRepository(Context context) { this.dao = AppDatabase.getInstance(context).recurringExpenseDao(); }
    long add(RecurringExpense e) { return dao.insert(e); }
    List<RecurringExpense> list(long userId) { return dao.list(userId); }
}
