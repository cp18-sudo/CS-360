package com.example.weighttracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

/**
 * Login and registration screen. Delegates all validation and auth logic
 * to {@link LoginViewModel} and reacts to single-consumption events for
 * navigation and error display.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText passwordEditText;
    private LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        AppDatabase db = AppDatabase.getDatabase(this);
        WeightRepository repository = new WeightRepository(db);
        viewModel = new ViewModelProvider(this, new LoginViewModelFactory(repository))
                .get(LoginViewModel.class);

        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.registerButton);

        loginButton.setOnClickListener(v -> viewModel.login(
                usernameEditText.getText().toString(),
                passwordEditText.getText().toString()));

        registerButton.setOnClickListener(v -> viewModel.register(
                usernameEditText.getText().toString(),
                passwordEditText.getText().toString()));

        viewModel.getLoginSuccess().observe(this, event -> {
            String userId = event.getContentIfNotConsumed();
            if (userId != null) {
                Intent intent = new Intent(this, DataGridActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getErrorMessage().observe(this, event -> {
            Integer resId = event.getContentIfNotConsumed();
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.getInfoMessage().observe(this, event -> {
            Integer resId = event.getContentIfNotConsumed();
            if (resId != null) {
                Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
