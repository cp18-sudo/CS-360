package com.example.weighttracker;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data-access object for weight entries. Provides full CRUD, a LiveData
 * query for reactive UI updates, and date-ordered queries for trend analysis.
 * All queries are scoped to a single user via userId.
 */
@Dao
public interface WeightDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertWeight(WeightEntry entry);

    @Update
    void updateWeight(WeightEntry entry);

    /** Reactive query; Room re-emits the list whenever the table changes. */
    @Query("SELECT * FROM weights WHERE userId = :userId ORDER BY date DESC")
    LiveData<List<WeightEntry>> getAllWeightsLive(String userId);

    /** Synchronous query for background-thread callers. */
    @Query("SELECT * FROM weights WHERE userId = :userId ORDER BY date DESC")
    List<WeightEntry> getAllWeights(String userId);

    /** All entries in ascending date order for trend analysis. */
    @Query("SELECT * FROM weights WHERE userId = :userId ORDER BY date ASC")
    List<WeightEntry> getAllWeightsAscending(String userId);

    /** Entries within a date range (inclusive), ascending. */
    @Query("SELECT * FROM weights WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    List<WeightEntry> getWeightsByDateRange(String userId, String startDate, String endDate);

    @Delete
    void deleteWeight(WeightEntry entry);

    /** Quick count for UI display. */
    @Query("SELECT COUNT(*) FROM weights WHERE userId = :userId")
    int getEntryCount(String userId);
}
