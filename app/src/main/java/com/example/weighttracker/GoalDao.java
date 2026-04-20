package com.example.weighttracker;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

/** Data-access object for goal weights. */
@Dao
public interface GoalDao {

    @Insert
    void insertGoal(Goal goal);

    @Update
    void updateGoal(Goal goal);

    /**
     * Returns the active goal for a user.
     *
     * The id tie-breaker matters because the app stores dates as yyyy-MM-dd.
     * If a user changes the goal twice in one day, the row inserted last should
     * be treated as the current goal.
     */
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdDate DESC, id DESC LIMIT 1")
    Goal getActiveGoal(String userId);

    @Delete
    void deleteGoal(Goal goal);
}
