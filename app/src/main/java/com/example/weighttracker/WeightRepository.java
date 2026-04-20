package com.example.weighttracker;

import androidx.lifecycle.LiveData;

import java.util.List;

/**
 * Single point of access for all persistent data. Sits between the
 * ViewModels and the Room DAOs so that the UI layer never touches
 * the database directly.
 *
 * This is the main MVVM boundary I use in the code review video: Activities
 * handle screens, ViewModels make decisions, and this repository owns data
 * access plus the bridge into trend analysis.
 */
public class WeightRepository {

    private final UserDao userDao;
    private final WeightDao weightDao;
    private final GoalDao goalDao;
    private final TrendCalculator trendCalculator;

    public WeightRepository(AppDatabase database) {
        userDao = database.userDao();
        weightDao = database.weightDao();
        goalDao = database.goalDao();
        trendCalculator = new TrendCalculator();
    }

    // ---- User operations (must be called off the main thread) ----

    /** Fetch a user by username for hash-based login verification. */
    public User getUserByUsername(String username) {
        return userDao.getUserByUsername(username);
    }

    /** Accepts a pre-hashed password so plain text never reaches Room. */
    public long registerUser(String username, String hashedPassword) {
        return userDao.insertUser(new User(username, hashedPassword));
    }

    // ---- Weight entry operations ----

    /**
     * LiveData query scoped to a user; Room handles background threading
     * automatically for observable queries.
     */
    public LiveData<List<WeightEntry>> getAllWeightsLive(String userId) {
        return weightDao.getAllWeightsLive(userId);
    }

    /** Must be called off the main thread. */
    public void insertWeight(WeightEntry entry) {
        weightDao.insertWeight(entry);
    }

    /** Must be called off the main thread. */
    public void updateWeight(WeightEntry entry) {
        weightDao.updateWeight(entry);
    }

    /** Must be called off the main thread. */
    public void deleteWeight(WeightEntry entry) {
        weightDao.deleteWeight(entry);
    }

    // ---- Goal operations (must be called off the main thread) ----

    public void insertGoal(Goal goal) {
        goalDao.insertGoal(goal);
    }

    public void updateGoal(Goal goal) {
        goalDao.updateGoal(goal);
    }

    /** Returns the most recent goal for the user, or null if none exists. */
    public Goal getActiveGoal(String userId) {
        return goalDao.getActiveGoal(userId);
    }

    // ---- Trend analysis ----

    /** Fetch all entries for a user, run trend analysis, return result. Must be called off the main thread. */
    public WeightTrendResult computeTrend(String userId, double goalWeight) {
        List<WeightEntry> entries = weightDao.getAllWeightsAscending(userId);
        return trendCalculator.analyze(entries, goalWeight);
    }

    /** Same as computeTrend but filtered to a date range. Must be called off the main thread. */
    public WeightTrendResult computeTrendForRange(String userId,
                                                   String startDate,
                                                   String endDate,
                                                   double goalWeight) {
        List<WeightEntry> entries =
                weightDao.getWeightsByDateRange(userId, startDate, endDate);
        return trendCalculator.analyze(entries, goalWeight);
    }
}
