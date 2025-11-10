package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RecurringExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecurringExpense item);

    @Update
    int update(RecurringExpense item);

    @Delete
    int delete(RecurringExpense item);

    @Query("SELECT * FROM recurring_expenses WHERE userId = :userId")
    List<RecurringExpense> list(long userId);
}
