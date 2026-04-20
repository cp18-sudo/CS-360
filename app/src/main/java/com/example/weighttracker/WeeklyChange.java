package com.example.weighttracker;

import java.util.Locale;

/**
 * Represents the change in average weight from one week to the next.
 * Positive delta = gained weight; negative = lost weight.
 */
public class WeeklyChange {

    private final String weekEndDate;
    private final double previousAverage;
    private final double currentAverage;
    private final double delta;
    private final double percentChange;

    public WeeklyChange(String weekEndDate, double previousAverage,
                        double currentAverage) {
        this.weekEndDate = weekEndDate;
        this.previousAverage = previousAverage;
        this.currentAverage = currentAverage;
        this.delta = currentAverage - previousAverage;
        this.percentChange = (previousAverage != 0)
                ? (delta / previousAverage) * 100.0
                : 0.0;
    }

    /** The last date in the current week window. */
    public String getWeekEndDate() {
        return weekEndDate;
    }

    public double getPreviousAverage() {
        return previousAverage;
    }

    public double getCurrentAverage() {
        return currentAverage;
    }

    /** Absolute change: current minus previous. */
    public double getDelta() {
        return delta;
    }

    /** Relative change as a percentage of the previous average. */
    public double getPercentChange() {
        return percentChange;
    }

    /** True if the user lost weight this week. */
    public boolean isLoss() {
        return delta < 0;
    }

    @Override
    public String toString() {
        String sign = (delta >= 0) ? "+" : "";
        return weekEndDate + ": " + sign + String.format(Locale.US, "%.1f", delta)
                + " lbs (" + sign + String.format(Locale.US, "%.1f", percentChange) + "%)";
    }
}
