package com.example.weighttracker;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Main screen showing weight entries in a list with add, edit, and delete
 * support. Observes LiveData from {@link DataGridViewModel} for automatic
 * UI refresh and single-consumption toast events. Also displays a trend
 * summary card with rolling average, direction, and alerts.
 *
 * In the final code review, this Activity shows the result of the MVVM
 * refactor: it mostly wires up Android UI pieces and leaves data decisions
 * to the ViewModel and Repository.
 */
public class DataGridActivity extends AppCompatActivity {

    private DataGridViewModel viewModel;
    private WeightAdapter adapter;

    // Trend summary views
    private View trendCard;
    private TextView trendAverageText;
    private TextView trendDirectionText;
    private TextView trendWeeklyChangeText;
    private TextView trendAlertText;
    private TextView trendInsufficientText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_grid);

        String userId = getIntent().getStringExtra("userId");

        AppDatabase db = AppDatabase.getDatabase(this);
        WeightRepository repository = new WeightRepository(db);
        viewModel = new ViewModelProvider(this, new DataGridViewModelFactory(repository, userId))
                .get(DataGridViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.weightRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WeightAdapter(
                entry -> viewModel.deleteWeightEntry(entry),
                this::showEditDialog
        );
        recyclerView.setAdapter(adapter);

        // Trend summary card views
        trendCard = findViewById(R.id.trendCard);
        trendAverageText = findViewById(R.id.trendAverageText);
        trendDirectionText = findViewById(R.id.trendDirectionText);
        trendWeeklyChangeText = findViewById(R.id.trendWeeklyChangeText);
        trendAlertText = findViewById(R.id.trendAlertText);
        trendInsufficientText = findViewById(R.id.trendInsufficientText);

        // --- Observe LiveData ---

        viewModel.getAllWeights().observe(this, entries -> {
            if (entries != null) {
                adapter.submitList(entries);
            }
        });

        viewModel.getToastMessage().observe(this, event -> {
            Integer resId = event.getContentIfNotConsumed();
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
            }
        });

        // Trend analysis result for the summary card
        viewModel.getTrendResult().observe(this, this::updateTrendCard);

        // Goal-reached event (driven by rolling-average comparison)
        viewModel.getGoalReachedEvent().observe(this, event -> {
            Boolean reached = event.getContentIfNotConsumed();
            if (reached != null && reached) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.weight_goal_reached,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        // --- Buttons ---

        Button addEntryButton = findViewById(R.id.addEntryButton);
        addEntryButton.setOnClickListener(v -> showAddDialog());

        Button setGoalButton = findViewById(R.id.setGoalButton);
        setGoalButton.setOnClickListener(v -> showGoalDialog());

        Button smsPermissionButton = findViewById(R.id.smsPermissionButton);
        smsPermissionButton.setOnClickListener(v ->
                startActivity(new Intent(this, SmsPermissionActivity.class)));
    }

    // ---- Trend card rendering ----

    /**
     * Update the trend summary card with the latest analysis results.
     * If there is not enough data, shows an informational message.
     */
    private void updateTrendCard(WeightTrendResult result) {
        if (result == null) {
            trendCard.setVisibility(View.GONE);
            return;
        }

        trendCard.setVisibility(View.VISIBLE);

        if (result.hasInsufficientData()) {
            trendInsufficientText.setVisibility(View.VISIBLE);
            trendInsufficientText.setText(getString(
                    R.string.trend_insufficient_data,
                    TrendCalculator.DEFAULT_WINDOW_SIZE));
            trendAverageText.setVisibility(View.GONE);
            trendDirectionText.setVisibility(View.GONE);
            trendWeeklyChangeText.setVisibility(View.GONE);
            trendAlertText.setVisibility(View.GONE);
            return;
        }

        trendInsufficientText.setVisibility(View.GONE);
        trendAverageText.setVisibility(View.VISIBLE);
        trendDirectionText.setVisibility(View.VISIBLE);

        // Rolling average
        trendAverageText.setText(String.format(
                getString(R.string.trend_rolling_average),
                result.getLatestRollingAverage()));

        // Trend direction
        trendDirectionText.setText(getString(
                R.string.trend_direction,
                formatDirection(result.getDirection())));

        // Weekly change and alerts only when there's enough data
        if (result.hasFullWeekData()) {
            List<WeeklyChange> changes = result.getWeeklyChanges();
            WeeklyChange latest = changes.get(changes.size() - 1);
            String sign = latest.getDelta() >= 0 ? "+" : "";
            trendWeeklyChangeText.setText(getString(
                    R.string.trend_weekly_change,
                    sign, latest.getDelta(),
                    sign, latest.getPercentChange()));
            trendWeeklyChangeText.setVisibility(View.VISIBLE);

            List<TrendAlert> alerts = result.getAlerts();
            if (!alerts.isEmpty()) {
                TrendAlert latestAlert = alerts.get(alerts.size() - 1);
                trendAlertText.setText(latestAlert.getMessage());
                trendAlertText.setVisibility(View.VISIBLE);
            } else {
                trendAlertText.setVisibility(View.GONE);
            }
        } else {
            trendWeeklyChangeText.setVisibility(View.GONE);
            trendAlertText.setVisibility(View.GONE);
        }
    }

    /** Convert the trend direction enum to a user-friendly string. */
    private String formatDirection(
            WeightTrendResult.TrendDirection direction) {
        switch (direction) {
            case LOSING:
                return getString(R.string.trend_losing);
            case GAINING:
                return getString(R.string.trend_gaining);
            case STABLE:
                return getString(R.string.trend_stable);
            default:
                return getString(R.string.trend_unknown);
        }
    }

    // ---- Dialogs ----

    private void showAddDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_weight_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(R.string.weight_hint);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            viewModel.addWeightEntry(input.getText().toString());
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showEditDialog(WeightEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit_weight_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.valueOf(entry.getWeight()));
        input.setSelection(input.getText().length());
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) ->
                viewModel.updateWeightEntry(entry, input.getText().toString()));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.set_goal_title);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint(R.string.weight_hint);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, (dialog, which) -> {
            WeightParseResult result = InputValidator.parseAndValidateWeight(
                    input.getText().toString());
            if (result.isValid()) {
                viewModel.setGoalWeight(result.getWeight());
                Toast.makeText(this, R.string.goal_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, result.getErrorResId(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }
}
