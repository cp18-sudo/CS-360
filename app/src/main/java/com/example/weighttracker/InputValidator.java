package com.example.weighttracker;

/**
 * Centralizes input validation rules. Returns Android string resource IDs
 * for error messages so all user-facing text stays in strings.xml.
 * A return value of 0 means the input is valid.
 */
public final class InputValidator {

    static final double MIN_WEIGHT = 1.0;
    static final double MAX_WEIGHT = 1500.0;
    static final int MIN_USERNAME_LENGTH = 3;
    static final int MAX_USERNAME_LENGTH = 30;
    static final int MIN_PASSWORD_LENGTH = 6;

    private InputValidator() {}

    /** Returns 0 if valid, or a string resource ID for the error. */
    public static int validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return R.string.error_username_required;
        }
        String trimmed = username.trim();
        if (trimmed.length() < MIN_USERNAME_LENGTH) {
            return R.string.error_username_too_short;
        }
        if (trimmed.length() > MAX_USERNAME_LENGTH) {
            return R.string.error_username_too_long;
        }
        return 0;
    }

    /** Returns 0 if valid, or a string resource ID for the error. */
    public static int validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return R.string.error_password_required;
        }
        if (password.trim().length() < MIN_PASSWORD_LENGTH) {
            return R.string.error_password_too_short;
        }
        return 0;
    }

    /**
     * Parses a weight string and validates the value is within range.
     * Returns a typed result instead of throwing, so callers handle
     * success and failure through the same code path.
     */
    public static WeightParseResult parseAndValidateWeight(String weightText) {
        if (weightText == null || weightText.trim().isEmpty()) {
            return WeightParseResult.error(R.string.error_weight_required);
        }
        double weight;
        try {
            weight = Double.parseDouble(weightText.trim());
        } catch (NumberFormatException e) {
            return WeightParseResult.error(R.string.error_weight_invalid);
        }
        if (weight < MIN_WEIGHT || weight > MAX_WEIGHT) {
            return WeightParseResult.error(R.string.error_weight_out_of_range);
        }
        return WeightParseResult.success(weight);
    }
}
