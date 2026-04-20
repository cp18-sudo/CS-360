package com.example.weighttracker;

import java.util.Collections;
import java.util.List;

/**
 * Bundles every piece of trend analysis into a single immutable object
 * that the ViewModel exposes via LiveData. Keeps the UI update atomic:
 * one new result, one redraw.
 */
public class WeightTrendResult {

    /** Overall direction the trend is heading. */
    public enum TrendDirection {
        LOSING,
        GAINING,
        STABLE,
        INSUFFICIENT_DATA
    }

    private final List<RollingAverage> rollingAverages;
    private final List<WeeklyChange> weeklyChanges;
    private final List<TrendAlert> alerts;
    private final TrendDirection direction;
    private final double latestRollingAverage;
    private final boolean goalReached;
    private final int totalEntries;

    public WeightTrendResult(List<RollingAverage> rollingAverages,
                             List<WeeklyChange> weeklyChanges,
                             List<TrendAlert> alerts,
                             TrendDirection direction,
                             double latestRollingAverage,
                             boolean goalReached,
                             int totalEntries) {
        this.rollingAverages = Collections.unmodifiableList(rollingAverages);
        this.weeklyChanges = Collections.unmodifiableList(weeklyChanges);
        this.alerts = Collections.unmodifiableList(alerts);
        this.direction = direction;
        this.latestRollingAverage = latestRollingAverage;
        this.goalReached = goalReached;
        this.totalEntries = totalEntries;
    }

    /** Smoothed average series, one per date with a full window. */
    public List<RollingAverage> getRollingAverages() {
        return rollingAverages;
    }

    /** Week-over-week deltas from consecutive rolling averages. */
    public List<WeeklyChange> getWeeklyChanges() {
        return weeklyChanges;
    }

    /** Alerts triggered by changes exceeding the healthy threshold. */
    public List<TrendAlert> getAlerts() {
        return alerts;
    }

    /** Overall direction based on the most recent weekly changes. */
    public TrendDirection getDirection() {
        return direction;
    }

    /** Most recent rolling average, or 0 if not enough data. */
    public double getLatestRollingAverage() {
        return latestRollingAverage;
    }

    /** True if the rolling average crossed below the goal weight. */
    public boolean isGoalReached() {
        return goalReached;
    }

    /** Total raw entries that were fed into the calculation. */
    public int getTotalEntries() {
        return totalEntries;
    }

    /** True if there weren't enough entries for any trend data. */
    public boolean hasInsufficientData() {
        return direction == TrendDirection.INSUFFICIENT_DATA;
    }

    /**
     * True if there are enough entries for meaningful weekly comparisons
     * and alerts. Below this threshold the UI should show the rolling
     * average and direction only, not week-over-week deltas.
     */
    public boolean hasFullWeekData() {
        return !weeklyChanges.isEmpty();
    }
}
