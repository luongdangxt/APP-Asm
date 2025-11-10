package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface SavingsMonthlyGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(SavingsMonthlyGoal goal);

    @Update
    int update(SavingsMonthlyGoal goal);

    @Query("SELECT * FROM savings_monthly_goals WHERE userId = :userId AND monthKey = :monthKey LIMIT 1")
    SavingsMonthlyGoal find(long userId, int monthKey);
}
