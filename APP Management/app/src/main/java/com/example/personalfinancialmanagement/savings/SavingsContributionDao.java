package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SavingsContributionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(SavingsContribution c);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings_contributions WHERE userId = :userId AND dateUtc BETWEEN :start AND :end")
    double sumForMonth(long userId, long start, long end);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings_contributions WHERE userId = :userId AND goalId = :goalId")
    double totalForGoal(long userId, long goalId);

    @Query("SELECT * FROM savings_contributions WHERE userId = :userId AND goalId = :goalId ORDER BY dateUtc DESC")
    List<SavingsContribution> listForGoal(long userId, long goalId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM savings_contributions WHERE userId = :userId AND isAuto = 1 AND dateUtc BETWEEN :start AND :end")
    double sumAutoForMonth(long userId, long start, long end);
}
