package com.example.weighttracker;

/**
 * Holds the result of parsing and validating a weight input string.
 * Either contains a valid weight value, or an error resource ID that
 * the UI layer can resolve into a user-facing message.
 */
public final class WeightParseResult {

    private final double weight;
    private final int errorResId; // 0 = no error

    private WeightParseResult(double weight, int errorResId) {
        this.weight = weight;
        this.errorResId = errorResId;
    }

    public static WeightParseResult success(double weight) {
        return new WeightParseResult(weight, 0);
    }

    public static WeightParseResult error(int errorResId) {
        return new WeightParseResult(0, errorResId);
    }

    public boolean isValid() { return errorResId == 0; }
    public double getWeight() { return weight; }
    public int getErrorResId() { return errorResId; }
}
