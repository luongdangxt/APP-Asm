package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SavingsGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SavingsGoal goal);

    @Update
    int update(SavingsGoal goal);

    @Delete
    int delete(SavingsGoal goal);

    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY createdAtUtc DESC")
    List<SavingsGoal> list(long userId);

    @Query("SELECT * FROM savings_goals WHERE id = :id LIMIT 1")
    SavingsGoal findById(long id);
}
