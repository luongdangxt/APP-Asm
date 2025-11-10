package com.example.personalfinancialmanagement;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "savings_goals",
        foreignKeys = @ForeignKey(
                entity = User.class,
                parentColumns = "id",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("userId")}
)
public class SavingsGoal {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long userId;

    @NonNull
    public String title;

    public double targetAmount;

    // optional icon key; keep simple string that maps to drawable name
    @NonNull
    public String iconKey;

    public long createdAtUtc;

    // deadline to complete the goal (UTC millis)
    public long deadlineUtc;

    // contribution cadence: 0=daily,1=weekly,2=monthly,3=yearly (reserved)
    public int cadence;

    public SavingsGoal(long userId, @NonNull String title, double targetAmount, @NonNull String iconKey, long createdAtUtc) {
        this.userId = userId;
        this.title = title;
        this.targetAmount = targetAmount;
        this.iconKey = iconKey;
        this.createdAtUtc = createdAtUtc;
    }
}
