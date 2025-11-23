package com.example.personalfinancialmanagement;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feedback")
public class Feedback {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public String message;
    public long createdAt;

    public Feedback(long userId, String message, long createdAt) {
        this.userId = userId;
        this.message = message;
        this.createdAt = createdAt;
    }
}
