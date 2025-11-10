package com.example.personalfinancialmanagement;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "savings_monthly_goals",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"userId", "monthKey"}, unique = true)}
)
public class SavingsMonthlyGoal {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    // YYYYMM
    public int monthKey;

    public double targetAmount;

    public SavingsMonthlyGoal(long userId, int monthKey, double targetAmount) {
        this.userId = userId;
        this.monthKey = monthKey;
        this.targetAmount = targetAmount;
    }
}
