package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(Budget budget);

    @Update
    int update(Budget budget);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthKey = :monthKey")
    List<Budget> listForMonth(long userId, int monthKey);

    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthKey = :monthKey AND category = :category LIMIT 1")
    Budget find(long userId, int monthKey, String category);
}
