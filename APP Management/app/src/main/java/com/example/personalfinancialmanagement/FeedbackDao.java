package com.example.personalfinancialmanagement;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FeedbackDao {
    @Insert
    long insert(Feedback f);

    @Query("SELECT * FROM feedback WHERE userId = :userId ORDER BY createdAt DESC")
    List<Feedback> list(long userId);
}
