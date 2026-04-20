package com.example.weighttracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Turns a list of raw weight entries into trend analysis data.
 * No Android dependencies, so it can be unit-tested without an emulator.
 *
 * Pipeline: TreeMap build -> rolling averages -> weekly changes -> alerts
 * -> trend direction -> goal check. Overall O(n log n) time, O(n) space.
 *
 * The code is intentionally plain Java so it can be tested from Android
 * Studio without launching the app or depending on Android framework classes.
 */
public class TrendCalculator {

    /** Default number of entries in the rolling-average window. */
    public static final int DEFAULT_WINDOW_SIZE = 7;

    /**
     * Default weekly change threshold in pounds. Exceeding this
     * triggers a WARNING; double triggers CRITICAL.
     */
    public static final double DEFAULT_ALERT_THRESHOLD = 2.0;

    private final int windowSize;
    private final double alertThreshold;

    /** Create a calculator with default window (7) and threshold (2.0 lbs). */
    public TrendCalculator() {
        this(DEFAULT_WINDOW_SIZE, DEFAULT_ALERT_THRESHOLD);
    }

    /** Create a calculator with custom window size and alert threshold. */
    public TrendCalculator(int windowSize, double alertThreshold) {
        if (windowSize < 2) {
            throw new IllegalArgumentException(
                    "Window size must be at least 2, got " + windowSize);
        }
        if (alertThreshold <= 0) {
            throw new IllegalArgumentException(
                    "Alert threshold must be positive, got " + alertThreshold);
        }
        this.windowSize = windowSize;
        this.alertThreshold = alertThreshold;
    }

    // ---- Public API ----

    /**
     * Run the full analysis pipeline on a list of weight entries.
     *
     * @param entries    raw entries (any order; duplicates OK)
     * @param goalWeight the user's target weight, or Double.NaN if unset
     * @return a WeightTrendResult with every computed metric
     */
    public WeightTrendResult analyze(List<WeightEntry> entries,
                                     double goalWeight) {
        if (entries == null || entries.size() < windowSize) {
            return buildInsufficientResult(entries == null ? 0 : entries.size());
        }

        // Step 1: Sort entries by date using a TreeMap.
        // yyyy-MM-dd strings sort chronologically, so no custom comparator needed.
        TreeMap<String, Double> sortedWeights = buildSortedMap(entries);

        List<String> dates = new ArrayList<>(sortedWeights.keySet());
        List<Double> weights = new ArrayList<>(sortedWeights.values());

        // Step 2: Sliding window rolling averages, O(n).
        List<RollingAverage> rollingAverages =
                computeRollingAverages(dates, weights);

        // Steps 3-4: Weekly changes and alerts only when there are enough
        // entries for a real week-over-week comparison (>= 2 * windowSize).
        // Below that threshold we show rolling average and direction only,
        // so we don't imply a full-week comparison from partial data.
        List<WeeklyChange> weeklyChanges;
        List<TrendAlert> alerts;
        if (entries.size() >= windowSize * 2) {
            weeklyChanges = computeWeeklyChanges(rollingAverages);
            alerts = generateAlerts(weeklyChanges);
        } else {
            weeklyChanges = Collections.emptyList();
            alerts = Collections.emptyList();
        }

        // Step 5: Trend direction from weekly changes if available,
        // otherwise fall back to rolling-average slope.
        WeightTrendResult.TrendDirection direction;
        if (!weeklyChanges.isEmpty()) {
            direction = determineTrendFromChanges(weeklyChanges);
        } else {
            direction = determineTrendFromAverages(rollingAverages);
        }

        // Step 6: Goal check using rolling average instead of raw entry.
        double latestAvg = rollingAverages.isEmpty() ? 0.0
                : rollingAverages.get(rollingAverages.size() - 1).getAverage();
        boolean goalReached = checkGoal(latestAvg, goalWeight);

        return new WeightTrendResult(
                rollingAverages,
                weeklyChanges,
                alerts,
                direction,
                latestAvg,
                goalReached,
                entries.size()
        );
    }

    // ---- Step 1: Sorted map ----

    /**
     * Build a TreeMap from raw entries. If multiple entries share a date,
     * the last one wins (simulates an edit). TreeMap keeps entries in
     * chronological order automatically at O(log n) per insert.
     */
    TreeMap<String, Double> buildSortedMap(List<WeightEntry> entries) {
        TreeMap<String, Double> map = new TreeMap<>();
        for (WeightEntry entry : entries) {
            map.put(entry.getDate(), entry.getWeight());
        }
        return map;
    }

    // ---- Step 2: Sliding-window rolling averages ----

    /**
     * Compute rolling averages with an incremental sliding window.
     * Maintains a running sum instead of re-scanning the window at
     * every position, so the whole pass is O(n).
     */
    List<RollingAverage> computeRollingAverages(List<String> dates,
                                                List<Double> weights) {
        int n = weights.size();
        List<RollingAverage> result = new ArrayList<>();

        if (n < windowSize) {
            return result;
        }

        // Seed the running sum with the first window.
        double windowSum = 0.0;
        for (int i = 0; i < windowSize; i++) {
            windowSum += weights.get(i);
        }
        result.add(new RollingAverage(
                dates.get(windowSize - 1),
                windowSum / windowSize,
                windowSize));

        // Slide forward: add the new element, subtract the one that left.
        for (int i = windowSize; i < n; i++) {
            windowSum += weights.get(i) - weights.get(i - windowSize);
            result.add(new RollingAverage(
                    dates.get(i),
                    windowSum / windowSize,
                    windowSize));
        }

        return result;
    }

    // ---- Step 3: Weekly changes ----

    /**
     * Compare rolling averages at weekly intervals to get deltas.
     * Needs more than windowSize rolling averages so the sampling loop
     * can compare at least two points one window apart.
     */
    List<WeeklyChange> computeWeeklyChanges(
            List<RollingAverage> rollingAverages) {
        List<WeeklyChange> changes = new ArrayList<>();

        if (rollingAverages.size() <= windowSize) {
            return changes;
        }

        // Sample at window-size intervals for full-week comparisons.
        for (int i = windowSize;
             i < rollingAverages.size();
             i += windowSize) {
            RollingAverage prev = rollingAverages.get(i - windowSize);
            RollingAverage curr = rollingAverages.get(i);
            changes.add(new WeeklyChange(
                    curr.getDate(),
                    prev.getAverage(),
                    curr.getAverage()));
        }

        return changes;
    }

    // ---- Step 4: Alert generation ----

    /**
     * Check each weekly change against the threshold.
     * |delta| > threshold = WARNING, |delta| > threshold * 2 = CRITICAL.
     */
    List<TrendAlert> generateAlerts(List<WeeklyChange> weeklyChanges) {
        List<TrendAlert> alerts = new ArrayList<>();

        for (WeeklyChange change : weeklyChanges) {
            double absDelta = Math.abs(change.getDelta());

            if (absDelta > alertThreshold * 2) {
                alerts.add(buildAlert(change, TrendAlert.Severity.CRITICAL));
            } else if (absDelta > alertThreshold) {
                alerts.add(buildAlert(change, TrendAlert.Severity.WARNING));
            }
        }

        return alerts;
    }

    /** Build a single alert from a weekly change and severity level. */
    private TrendAlert buildAlert(WeeklyChange change,
                                  TrendAlert.Severity severity) {
        TrendAlert.Direction dir = change.isLoss()
                ? TrendAlert.Direction.LOSS
                : TrendAlert.Direction.GAIN;

        String action = change.isLoss() ? "lost" : "gained";
        String msg = String.format(Locale.US,
                "You %s %.1f lbs in the week ending %s. "
                        + "Changes over %.1f lbs/week may need attention.",
                action,
                Math.abs(change.getDelta()),
                change.getWeekEndDate(),
                alertThreshold);

        return new TrendAlert(
                change.getWeekEndDate(),
                change.getDelta(),
                severity,
                dir,
                msg);
    }

    // ---- Step 5: Trend direction ----

    /**
     * Determine trend from weekly changes. Looks at the most recent
     * changes (up to 3) and uses a simple majority. A 0.1 lb threshold
     * filters out noise.
     */
    WeightTrendResult.TrendDirection determineTrendFromChanges(
            List<WeeklyChange> weeklyChanges) {
        if (weeklyChanges.isEmpty()) {
            return WeightTrendResult.TrendDirection.INSUFFICIENT_DATA;
        }

        int lookback = Math.min(3, weeklyChanges.size());
        int losing = 0;
        int gaining = 0;
        double stabilityThreshold = 0.1;

        for (int i = weeklyChanges.size() - lookback;
             i < weeklyChanges.size(); i++) {
            double delta = weeklyChanges.get(i).getDelta();
            if (delta < -stabilityThreshold) {
                losing++;
            } else if (delta > stabilityThreshold) {
                gaining++;
            }
        }

        if (losing > gaining) {
            return WeightTrendResult.TrendDirection.LOSING;
        } else if (gaining > losing) {
            return WeightTrendResult.TrendDirection.GAINING;
        } else {
            return WeightTrendResult.TrendDirection.STABLE;
        }
    }

    /**
     * Fallback trend detection from rolling averages when there aren't
     * enough entries for weekly comparisons. Compares the first and
     * last rolling average to determine overall direction.
     */
    WeightTrendResult.TrendDirection determineTrendFromAverages(
            List<RollingAverage> rollingAverages) {
        if (rollingAverages.size() < 2) {
            return WeightTrendResult.TrendDirection.STABLE;
        }

        double first = rollingAverages.get(0).getAverage();
        double last = rollingAverages.get(rollingAverages.size() - 1).getAverage();
        double diff = last - first;

        if (diff < -0.1) {
            return WeightTrendResult.TrendDirection.LOSING;
        } else if (diff > 0.1) {
            return WeightTrendResult.TrendDirection.GAINING;
        } else {
            return WeightTrendResult.TrendDirection.STABLE;
        }
    }

    // ---- Step 6: Goal comparison ----

    /**
     * Check whether the rolling average has reached the goal.
     * Uses <= instead of == so crossing below the target counts.
     * Returns false if no goal is set (NaN) or average is zero.
     */
    boolean checkGoal(double latestRollingAverage, double goalWeight) {
        if (Double.isNaN(goalWeight) || latestRollingAverage <= 0) {
            return false;
        }
        return latestRollingAverage <= goalWeight;
    }

    // ---- Insufficient-data fallback ----

    /** Return an empty result when there aren't enough entries. */
    private WeightTrendResult buildInsufficientResult(int totalEntries) {
        return new WeightTrendResult(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                WeightTrendResult.TrendDirection.INSUFFICIENT_DATA,
                0.0,
                false,
                totalEntries
        );
    }
}
