package com.example.weighttracker;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Room entity for the weights table. Stores one weight reading
 * with its date, scoped to a user via a foreign key. The setId()
 * method exists so Room can match an updated object to its existing row.
 *
 * The userId and date indexes support the database enhancement: each account
 * sees only its own entries, and trend/date-range queries can stay efficient.
 */
@Entity(tableName = "weights",
        foreignKeys = @ForeignKey(entity = User.class,
                parentColumns = "username",
                childColumns = "userId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "userId"), @Index(value = {"userId", "date"})})
public class WeightEntry {

    @PrimaryKey(autoGenerate = true)
    private int id = 0;

    @NonNull
    private final String userId;

    @NonNull
    private final String date;

    private final double weight;

    public WeightEntry(@NonNull String userId, @NonNull String date, double weight) {
        this.userId = userId;
        this.date = date;
        this.weight = weight;
    }

    public int getId() { return id; }
    @NonNull public String getUserId() { return userId; }
    @NonNull public String getDate() { return date; }
    public double getWeight() { return weight; }

    public void setId(int id) { this.id = id; }
}
