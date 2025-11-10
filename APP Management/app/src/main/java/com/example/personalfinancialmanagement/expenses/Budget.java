package com.example.personalfinancialmanagement;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "budgets",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId"), @Index(value = {"userId", "monthKey", "category"}, unique = true)}
)
public class Budget {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    // monthKey format: YYYYMM, e.g., 202510
    public int monthKey;

    @NonNull
    public String category;

    public double limitAmount;

    public Budget(long userId, int monthKey, @NonNull String category, double limitAmount) {
        this.userId = userId;
        this.monthKey = monthKey;
        this.category = category;
        this.limitAmount = limitAmount;
    }
}
