package com.example.personalfinancialmanagement;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "expenses",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId"), @Index("category")}
)
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    @NonNull
    public String description;

    public long dateUtc; // millis since epoch

    public double amount;

    @NonNull
    public String category; // e.g., Food, Rent

    public Expense(long userId, @NonNull String description, long dateUtc, double amount, @NonNull String category) {
        this.userId = userId;
        this.description = description;
        this.dateUtc = dateUtc;
        this.amount = amount;
        this.category = category;
    }
}
