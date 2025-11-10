package com.example.personalfinancialmanagement;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recurring_expenses",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId"), @Index("category")}
)
public class RecurringExpense {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    @NonNull
    public String description;

    public double amount;

    @NonNull
    public String category;

    public long startDateUtc;
    public Long endDateUtc; // nullable indicates ongoing

    public int dayOfMonth; // when to apply each month (1-28 recommended)

    public RecurringExpense(long userId, @NonNull String description, double amount, @NonNull String category,
                             long startDateUtc, Long endDateUtc, int dayOfMonth) {
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.startDateUtc = startDateUtc;
        this.endDateUtc = endDateUtc;
        this.dayOfMonth = dayOfMonth;
    }
}
