package com.example.personalfinancialmanagement;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "savings_contributions",
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = SavingsGoal.class, parentColumns = "id", childColumns = "goalId", onDelete = ForeignKey.SET_NULL)
        },
        indices = {@Index("userId"), @Index("goalId"), @Index("dateUtc")}
)
public class SavingsContribution {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    public Long goalId; // nullable for unassigned savings

    public double amount; // positive number

    public long dateUtc;

    // whether this row was auto-created by the goal scheduler
    public boolean isAuto;

    public SavingsContribution(long userId, Long goalId, double amount, long dateUtc) {
        this.userId = userId;
        this.goalId = goalId;
        this.amount = amount;
        this.dateUtc = dateUtc;
        this.isAuto = false;
    }
}
