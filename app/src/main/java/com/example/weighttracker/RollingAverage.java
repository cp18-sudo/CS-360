package com.example.weighttracker;

import java.util.Locale;

/**
 * Holds a single rolling-average data point: one date paired with
 * the mean weight over the preceding window of entries.
 */
public class RollingAverage {

    private final String date;
    private final double average;
    private final int windowSize;

    public RollingAverage(String date, double average, int windowSize) {
        this.date = date;
        this.average = average;
        this.windowSize = windowSize;
    }

    public String getDate() {
        return date;
    }

    /** The mean weight for the window ending on this date. */
    public double getAverage() {
        return average;
    }

    /** How many entries were included in the window (e.g. 7). */
    public int getWindowSize() {
        return windowSize;
    }

    @Override
    public String toString() {
        return date + ": " + String.format(Locale.US, "%.1f", average)
                + " (" + windowSize + "-day avg)";
    }
}
