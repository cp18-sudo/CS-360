package com.example.weighttracker;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * Unit tests for TrendCalculator. Covers each stage of the analysis
 * pipeline: sorted map construction, rolling averages, weekly changes,
 * alerts, trend direction, and goal comparison.
 */
public class TrendCalculatorTest {

    private TrendCalculator calculator;

    @Before
    public void setUp() {
        calculator = new TrendCalculator();
    }

    // ---- Helpers ----

    private WeightEntry entry(String date, double weight) {
        return new WeightEntry("testUser", date, weight);
    }

    /** Build entries with sequential dates and the given weights. */
    private List<WeightEntry> buildEntries(double... weights) {
        List<WeightEntry> entries = new ArrayList<>();
        for (int i = 0; i < weights.length; i++) {
            String date = String.format("2025-01-%02d", i + 1);
            entries.add(entry(date, weights[i]));
        }
        return entries;
    }

    // ---- Null / empty / insufficient data ----

    @Test
    public void analyze_nullEntries_returnsInsufficientData() {
        WeightTrendResult result = calculator.analyze(null, 150.0);
        assertTrue(result.hasInsufficientData());
        assertEquals(0, result.getTotalEntries());
        assertTrue(result.getRollingAverages().isEmpty());
    }

    @Test
    public void analyze_emptyList_returnsInsufficientData() {
        WeightTrendResult result = calculator.analyze(
                Collections.emptyList(), 150.0);
        assertTrue(result.hasInsufficientData());
    }

    @Test
    public void analyze_fewerThanWindow_returnsInsufficientData() {
        List<WeightEntry> entries = buildEntries(180, 179, 178, 177, 176, 175);
        WeightTrendResult result = calculator.analyze(entries, 150.0);
        assertTrue(result.hasInsufficientData());
        assertEquals(6, result.getTotalEntries());
    }

    // ---- Sorted map construction ----

    @Test
    public void buildSortedMap_ordersByDateAscending() {
        List<WeightEntry> entries = Arrays.asList(
                entry("2025-01-03", 178),
                entry("2025-01-01", 180),
                entry("2025-01-02", 179));

        TreeMap<String, Double> map = calculator.buildSortedMap(entries);
        List<String> keys = new ArrayList<>(map.keySet());

        assertEquals("2025-01-01", keys.get(0));
        assertEquals("2025-01-02", keys.get(1));
        assertEquals("2025-01-03", keys.get(2));
    }

    @Test
    public void buildSortedMap_duplicateDates_lastWriteWins() {
        List<WeightEntry> entries = Arrays.asList(
                entry("2025-01-01", 180),
                entry("2025-01-01", 175));

        TreeMap<String, Double> map = calculator.buildSortedMap(entries);

        assertEquals(1, map.size());
        assertEquals(175.0, map.get("2025-01-01"), 0.001);
    }

    // ---- Rolling averages ----

    @Test
    public void rollingAverages_exactlyWindowSize_returnsOneAverage() {
        List<WeightEntry> entries = buildEntries(180, 179, 178, 177, 176, 175, 174);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertEquals(1, result.getRollingAverages().size());
        assertEquals(177.0, result.getRollingAverages().get(0).getAverage(), 0.001);
        assertEquals("2025-01-07", result.getRollingAverages().get(0).getDate());
    }

    @Test
    public void rollingAverages_constantWeight_equalsWeight() {
        List<WeightEntry> entries = buildEntries(150, 150, 150, 150, 150, 150, 150, 150, 150, 150);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        for (RollingAverage avg : result.getRollingAverages()) {
            assertEquals(150.0, avg.getAverage(), 0.001);
        }
    }

    @Test
    public void rollingAverages_slidingWindowIsIncremental() {
        List<WeightEntry> entries = buildEntries(180, 179, 178, 177, 176, 175, 174, 173, 172, 171);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertEquals(4, result.getRollingAverages().size());
        assertEquals(177.0, result.getRollingAverages().get(0).getAverage(), 0.001);
        assertEquals(176.0, result.getRollingAverages().get(1).getAverage(), 0.001);
        assertEquals(175.0, result.getRollingAverages().get(2).getAverage(), 0.001);
        assertEquals(174.0, result.getRollingAverages().get(3).getAverage(), 0.001);
    }

    @Test
    public void rollingAverages_customWindowSize() {
        TrendCalculator calc3 = new TrendCalculator(3, 2.0);
        List<WeightEntry> entries = buildEntries(180, 178, 176, 174, 172);
        WeightTrendResult result = calc3.analyze(entries, Double.NaN);

        assertEquals(3, result.getRollingAverages().size());
        assertEquals(178.0, result.getRollingAverages().get(0).getAverage(), 0.001);
    }

    // ---- Weekly change suppression for < 14 entries ----

    @Test
    public void analyze_fewerThan14Entries_suppressesWeeklyChanges() {
        // 10 entries: enough for rolling averages but not for weekly comparisons
        List<WeightEntry> entries = buildEntries(180, 179, 178, 177, 176, 175, 174, 173, 172, 171);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertFalse(result.hasInsufficientData());
        assertFalse(result.getRollingAverages().isEmpty());
        assertTrue(result.getWeeklyChanges().isEmpty());
        assertTrue(result.getAlerts().isEmpty());
        assertFalse(result.hasFullWeekData());
    }

    @Test
    public void analyze_fewerThan14Entries_stillShowsTrendDirection() {
        // 10 entries trending down: direction should still be LOSING
        List<WeightEntry> entries = buildEntries(180, 179, 178, 177, 176, 175, 174, 173, 172, 171);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertEquals(WeightTrendResult.TrendDirection.LOSING, result.getDirection());
    }

    // ---- Weekly changes (>= 14 entries) ----

    @Test
    public void weeklyChanges_twoFullWeeks_producesChange() {
        double[] weights = new double[14];
        for (int i = 0; i < 7; i++) weights[i] = 180.0;
        for (int i = 7; i < 14; i++) weights[i] = 175.0;

        List<WeightEntry> entries = buildEntries(weights);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertTrue(result.hasFullWeekData());
        assertFalse(result.getWeeklyChanges().isEmpty());
        WeeklyChange change = result.getWeeklyChanges().get(0);
        assertEquals(-5.0, change.getDelta(), 0.5);
        assertTrue(change.isLoss());
    }

    @Test
    public void weeklyChanges_percentChangeCalculation() {
        WeeklyChange change = new WeeklyChange("2025-01-14", 200.0, 190.0);
        assertEquals(-10.0, change.getDelta(), 0.001);
        assertEquals(-5.0, change.getPercentChange(), 0.001);
        assertTrue(change.isLoss());
    }

    @Test
    public void weeklyChanges_zeroPreviousAverage_noException() {
        WeeklyChange change = new WeeklyChange("2025-01-14", 0.0, 150.0);
        assertEquals(150.0, change.getDelta(), 0.001);
        assertEquals(0.0, change.getPercentChange(), 0.001);
    }

    // ---- Alerts ----

    @Test
    public void alerts_smallChange_noAlert() {
        WeeklyChange small = new WeeklyChange("2025-01-14", 180.0, 178.5);
        List<TrendAlert> alerts = calculator.generateAlerts(
                Collections.singletonList(small));
        assertTrue(alerts.isEmpty());
    }

    @Test
    public void alerts_moderateChange_warningAlert() {
        WeeklyChange moderate = new WeeklyChange("2025-01-14", 180.0, 177.0);
        List<TrendAlert> alerts = calculator.generateAlerts(
                Collections.singletonList(moderate));

        assertEquals(1, alerts.size());
        assertEquals(TrendAlert.Severity.WARNING, alerts.get(0).getSeverity());
        assertEquals(TrendAlert.Direction.LOSS, alerts.get(0).getDirection());
    }

    @Test
    public void alerts_largeChange_criticalAlert() {
        WeeklyChange large = new WeeklyChange("2025-01-14", 175.0, 180.0);
        List<TrendAlert> alerts = calculator.generateAlerts(
                Collections.singletonList(large));

        assertEquals(1, alerts.size());
        assertEquals(TrendAlert.Severity.CRITICAL, alerts.get(0).getSeverity());
        assertEquals(TrendAlert.Direction.GAIN, alerts.get(0).getDirection());
    }

    @Test
    public void alerts_exactlyAtThreshold_noAlert() {
        WeeklyChange exact = new WeeklyChange("2025-01-14", 180.0, 178.0);
        List<TrendAlert> alerts = calculator.generateAlerts(
                Collections.singletonList(exact));
        assertTrue(alerts.isEmpty());
    }

    // ---- Trend direction ----

    @Test
    public void trendDirection_consistentLoss_returnsLosing() {
        double[] weights = new double[21];
        for (int i = 0; i < 21; i++) weights[i] = 200.0 - i;

        WeightTrendResult result = calculator.analyze(buildEntries(weights), Double.NaN);
        assertEquals(WeightTrendResult.TrendDirection.LOSING, result.getDirection());
    }

    @Test
    public void trendDirection_consistentGain_returnsGaining() {
        double[] weights = new double[21];
        for (int i = 0; i < 21; i++) weights[i] = 150.0 + i;

        WeightTrendResult result = calculator.analyze(buildEntries(weights), Double.NaN);
        assertEquals(WeightTrendResult.TrendDirection.GAINING, result.getDirection());
    }

    @Test
    public void trendDirection_flatWeight_returnsStable() {
        double[] weights = new double[21];
        java.util.Arrays.fill(weights, 175.0);

        WeightTrendResult result = calculator.analyze(buildEntries(weights), Double.NaN);
        assertEquals(WeightTrendResult.TrendDirection.STABLE, result.getDirection());
    }

    @Test
    public void trendFromAverages_downwardSlope_returnsLosing() {
        // 8 entries (< 14), steadily dropping, so it uses average-based trend.
        List<WeightEntry> entries = buildEntries(185, 184, 183, 182, 181, 180, 179, 178);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertTrue(result.getWeeklyChanges().isEmpty());
        assertEquals(WeightTrendResult.TrendDirection.LOSING, result.getDirection());
    }

    @Test
    public void trendFromAverages_flatData_returnsStable() {
        List<WeightEntry> entries = buildEntries(175, 175, 175, 175, 175, 175, 175, 175);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertEquals(WeightTrendResult.TrendDirection.STABLE, result.getDirection());
    }

    // ---- Goal comparison ----

    @Test
    public void goalCheck_belowGoal_returnsTrue() {
        assertTrue(calculator.checkGoal(148.0, 150.0));
    }

    @Test
    public void goalCheck_equalsGoal_returnsTrue() {
        assertTrue(calculator.checkGoal(150.0, 150.0));
    }

    @Test
    public void goalCheck_aboveGoal_returnsFalse() {
        assertFalse(calculator.checkGoal(155.0, 150.0));
    }

    @Test
    public void goalCheck_nanGoal_returnsFalse() {
        assertFalse(calculator.checkGoal(148.0, Double.NaN));
    }

    @Test
    public void goalCheck_zeroAverage_returnsFalse() {
        assertFalse(calculator.checkGoal(0.0, 150.0));
    }

    // ---- Full pipeline ----

    @Test
    public void analyze_realisticData_producesExpectedResults() {
        double[] weights = {
                185.2, 184.8, 185.0, 184.5, 184.1, 183.8, 183.5,
                183.2, 182.9, 183.1, 182.6, 182.2, 181.8, 181.5,
                181.2, 180.8, 181.0, 180.5, 180.1, 179.8, 179.5
        };

        WeightTrendResult result = calculator.analyze(buildEntries(weights), 170.0);

        assertFalse(result.hasInsufficientData());
        assertEquals(21, result.getTotalEntries());
        assertFalse(result.getRollingAverages().isEmpty());
        assertTrue(result.hasFullWeekData());
        assertEquals(WeightTrendResult.TrendDirection.LOSING, result.getDirection());
        assertFalse(result.isGoalReached());
        assertTrue(result.getLatestRollingAverage() > 179.0);
        assertTrue(result.getLatestRollingAverage() < 182.0);
    }

    @Test
    public void analyze_goalReachedViaTrend() {
        double[] weights = {
                178, 177, 176, 175, 174, 173, 172,
                171, 170, 169, 168, 167, 166, 165
        };

        WeightTrendResult result = calculator.analyze(buildEntries(weights), 175.0);
        assertTrue(result.isGoalReached());
    }

    @Test
    public void analyze_unorderedEntries_stillCorrect() {
        List<WeightEntry> entries = Arrays.asList(
                entry("2025-01-05", 176),
                entry("2025-01-01", 180),
                entry("2025-01-07", 174),
                entry("2025-01-03", 178),
                entry("2025-01-06", 175),
                entry("2025-01-02", 179),
                entry("2025-01-04", 177));

        WeightTrendResult result = calculator.analyze(entries, Double.NaN);

        assertEquals(1, result.getRollingAverages().size());
        assertEquals(177.0, result.getRollingAverages().get(0).getAverage(), 0.001);
    }

    // ---- Immutability ----

    @Test(expected = UnsupportedOperationException.class)
    public void resultLists_areUnmodifiable() {
        List<WeightEntry> entries = buildEntries(180, 179, 178, 177, 176, 175, 174);
        WeightTrendResult result = calculator.analyze(entries, Double.NaN);
        result.getRollingAverages().add(new RollingAverage("2025-01-08", 999.0, 7));
    }

    // ---- Constructor validation ----

    @Test(expected = IllegalArgumentException.class)
    public void constructor_windowSizeTooSmall_throws() {
        new TrendCalculator(1, 2.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_negativeThreshold_throws() {
        new TrendCalculator(7, -1.0);
    }
}
