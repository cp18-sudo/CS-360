package com.example.weighttracker;

/**
 * An alert generated when a weekly weight change exceeds the
 * configured healthy threshold. Carries a severity, direction,
 * and a ready-to-display message for the UI.
 */
public class TrendAlert {

    public enum Severity {
        INFO,
        WARNING,
        CRITICAL
    }

    public enum Direction {
        GAIN,
        LOSS
    }

    private final String weekEndDate;
    private final double weeklyDelta;
    private final Severity severity;
    private final Direction direction;
    private final String message;

    public TrendAlert(String weekEndDate, double weeklyDelta,
                      Severity severity, Direction direction, String message) {
        this.weekEndDate = weekEndDate;
        this.weeklyDelta = weeklyDelta;
        this.severity = severity;
        this.direction = direction;
        this.message = message;
    }

    public String getWeekEndDate() {
        return weekEndDate;
    }

    public double getWeeklyDelta() {
        return weeklyDelta;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Direction getDirection() {
        return direction;
    }

    /** Ready-to-display message describing the alert. */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + severity + "] " + message;
    }
}
