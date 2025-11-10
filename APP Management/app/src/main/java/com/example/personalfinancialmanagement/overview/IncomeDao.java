package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface IncomeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Income income);

    @Query("SELECT * FROM incomes WHERE userId = :userId ORDER BY dateUtc DESC LIMIT :limit")
    List<Income> latest(long userId, int limit);

    @Query("SELECT * FROM incomes WHERE userId = :userId AND dateUtc BETWEEN :start AND :end ORDER BY dateUtc DESC")
    List<Income> listByDateRange(long userId, long start, long end);
}
