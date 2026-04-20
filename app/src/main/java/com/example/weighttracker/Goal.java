package com.example.weighttracker;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for the goals table. Tracks each user's target weight.
 *
 * Keeping goals in their own table closes the original CS 360 gap where the
 * target was hardcoded in the Activity instead of being saved per user.
 */
@Entity(tableName = "goals",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "username",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = @Index(value = "userId"))
public class Goal {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private final String userId;

    private final double targetWeight;

    private final String createdDate;

    public Goal(@NonNull String userId, double targetWeight, String createdDate) {
        this.userId = userId;
        this.targetWeight = targetWeight;
        this.createdDate = createdDate;
    }

    public int getId() { return id; }
    @NonNull public String getUserId() { return userId; }
    public double getTargetWeight() { return targetWeight; }
    public String getCreatedDate() { return createdDate; }

    public void setId(int id) { this.id = id; }
}
