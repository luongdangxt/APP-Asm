package com.example.personalfinancialmanagement;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "incomes",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId"), @Index("category")}
)
public class Income {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    @NonNull
    public String title;

    public long dateUtc; // millis since epoch

    public double amount;

    @NonNull
    public String category; // Salary, Rewards, ...

    public Income(long userId, @NonNull String title, long dateUtc, double amount, @NonNull String category) {
        this.userId = userId;
        this.title = title;
        this.dateUtc = dateUtc;
        this.amount = amount;
        this.category = category;
    }
}
