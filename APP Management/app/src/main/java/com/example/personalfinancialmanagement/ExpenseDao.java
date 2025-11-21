package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Expense expense);

    @Update
    int update(Expense expense);

    @Delete
    int delete(Expense expense);

    @Query("SELECT * FROM expenses WHERE userId = :userId AND dateUtc BETWEEN :start AND :end ORDER BY dateUtc DESC")
    List<Expense> listByDateRange(long userId, long start, long end);

    @Query("SELECT COALESCE(SUM(amount),0) FROM expenses WHERE userId = :userId AND category = :category AND strftime('%Y%m', dateUtc/1000, 'unixepoch') = :monthKeyStr")
    double sumByCategoryForMonth(long userId, String category, String monthKeyStr);

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY dateUtc DESC LIMIT :limit")
    java.util.List<Expense> latest(long userId, int limit);
}
