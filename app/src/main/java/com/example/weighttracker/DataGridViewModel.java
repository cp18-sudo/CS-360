package com.example.weighttracker;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Screen-level logic for the weight-entry list. Exposes weight data as
 * LiveData for automatic UI updates and provides validated add/edit/delete
 * operations. Toast-style feedback goes through single-consumption events
 * so messages don't replay on rotation.
 *
 * This is where the three enhancements come together: the ViewModel keeps
 * Activity code small, validates user input before database writes, and
 * refreshes the trend analysis whenever the user's data changes.
 */
public class DataGridViewModel extends ViewModel {

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    /** Goal weight loaded from the database; defaults to 150.0 if no goal exists. */
    private double goalWeight = 150.0;

    /** Tracks whether the goal was already reached so the event only fires on the false->true crossing. */
    private boolean previousGoalState = false;

    private final WeightRepository repository;
    private final String userId;
    private final LiveData<List<WeightEntry>> allWeights;
    private final MutableLiveData<SingleEvent<Integer>> toastMessage = new MutableLiveData<>();

    /** Full trend analysis result for the summary card. */
    private final MutableLiveData<WeightTrendResult> trendResult = new MutableLiveData<>();

    /** One-shot event when the rolling average first crosses below the goal. */
    private final MutableLiveData<SingleEvent<Boolean>> goalReachedEvent = new MutableLiveData<>();

    public DataGridViewModel(WeightRepository repository, String userId) {
        this.repository = repository;
        this.userId = userId;
        allWeights = repository.getAllWeightsLive(userId);

        // Load goal from DB, then run initial trend analysis
        DbExecutors.io().execute(() -> {
            Goal goal = repository.getActiveGoal(userId);
            if (goal != null) {
                goalWeight = goal.getTargetWeight();
            }
            refreshTrend();
        });
    }

    // ---- LiveData getters ----

    /** Observable list of all weight entries, updated automatically by Room. */
    public LiveData<List<WeightEntry>> getAllWeights() {
        return allWeights;
    }

    /** One-shot toast feedback (string resource ID). */
    public LiveData<SingleEvent<Integer>> getToastMessage() {
        return toastMessage;
    }

    /** The full trend analysis result for the summary display. */
    public LiveData<WeightTrendResult> getTrendResult() {
        return trendResult;
    }

    /** One-shot event when the rolling average first reaches the goal. */
    public LiveData<SingleEvent<Boolean>> getGoalReachedEvent() {
        return goalReachedEvent;
    }

    // ---- Goal weight ----

    public double getGoalWeight() {
        return goalWeight;
    }

    /** Persist a new goal and refresh trend analysis with the updated target. */
    public void setGoalWeight(double goalWeight) {
        this.goalWeight = goalWeight;
        previousGoalState = false;
        DbExecutors.io().execute(() -> {
            // The DAO orders by date and id, so repeated goal changes on the
            // same day still load the newest target after restart.
            String date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
            Goal goal = new Goal(userId, goalWeight, date);
            repository.insertGoal(goal);
            refreshTrend();
        });
    }

    // ---- CRUD operations ----

    /**
     * Validates the input, creates an entry dated today, and inserts it.
     * Returns the parsed weight on success, or -1 if validation failed.
     */
    public double addWeightEntry(String weightText) {
        WeightParseResult result = InputValidator.parseAndValidateWeight(weightText);
        if (!result.isValid()) {
            toastMessage.setValue(new SingleEvent<>(result.getErrorResId()));
            return -1;
        }

        double weight = result.getWeight();
        String date = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(new Date());
        WeightEntry entry = new WeightEntry(userId, date, weight);

        DbExecutors.io().execute(() -> {
            repository.insertWeight(entry);
            refreshTrend();
        });
        return weight;
    }

    /**
     * Validates the new value and updates the entry in place, keeping
     * its original ID and date.
     */
    public void updateWeightEntry(WeightEntry entry, String newWeightText) {
        WeightParseResult result = InputValidator.parseAndValidateWeight(newWeightText);
        if (!result.isValid()) {
            toastMessage.setValue(new SingleEvent<>(result.getErrorResId()));
            return;
        }

        WeightEntry updated = new WeightEntry(userId, entry.getDate(), result.getWeight());
        updated.setId(entry.getId());

        DbExecutors.io().execute(() -> {
            repository.updateWeight(updated);
            toastMessage.postValue(new SingleEvent<>(R.string.entry_updated));
            refreshTrend();
        });
    }

    /** Delete an entry and refresh the trend analysis. */
    public void deleteWeightEntry(WeightEntry entry) {
        DbExecutors.io().execute(() -> {
            repository.deleteWeight(entry);
            refreshTrend();
        });
    }

    // ---- Trend analysis ----

    /**
     * Run trend analysis and post the result. Fires goalReachedEvent
     * only on a false->true crossing so the toast fires once.
     * Must be called from a background thread.
     */
    private void refreshTrend() {
        DbExecutors.io().execute(() -> {
            try {
                WeightTrendResult result = repository.computeTrend(userId, goalWeight);
                trendResult.postValue(result);

                // Only fire the event when the goal state transitions from
                // not-reached to reached, not on every refresh while the
                // average stays below the target.
                boolean currentGoalState = result.isGoalReached();
                if (currentGoalState && !previousGoalState) {
                    goalReachedEvent.postValue(new SingleEvent<>(true));
                }
                previousGoalState = currentGoalState;
            } catch (Exception e) {
                // Trend analysis failing shouldn't block the list display
            }
        });
    }
}
