package com.example.weighttracker;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Screen-level logic for login and registration. Validates input,
 * delegates to the repository, and exposes results as single-consumption
 * events so they don't replay on configuration changes. Passwords are
 * hashed with SHA-256 before storage.
 *
 * This class is a good place to show the MVVM split in Android Studio:
 * LoginActivity only reads fields and observes events, while the ViewModel
 * owns validation, background work, and authentication decisions.
 */
public class LoginViewModel extends ViewModel {

    private final WeightRepository repository;

    private final MutableLiveData<SingleEvent<String>> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Integer>> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<SingleEvent<Integer>> infoMessage = new MutableLiveData<>();

    public LoginViewModel(WeightRepository repository) {
        this.repository = repository;
    }

    /** Fires once with the username on successful login. */
    public LiveData<SingleEvent<String>> getLoginSuccess() { return loginSuccess; }

    /** Fires once with a string resource ID when something fails. */
    public LiveData<SingleEvent<Integer>> getErrorMessage() { return errorMessage; }

    /** Fires once with a string resource ID for non-error feedback. */
    public LiveData<SingleEvent<Integer>> getInfoMessage() { return infoMessage; }

    public void login(String username, String password) {
        int usernameError = InputValidator.validateUsername(username);
        if (usernameError != 0) {
            errorMessage.setValue(new SingleEvent<>(usernameError));
            return;
        }
        int passwordError = InputValidator.validatePassword(password);
        if (passwordError != 0) {
            errorMessage.setValue(new SingleEvent<>(passwordError));
            return;
        }

        DbExecutors.io().execute(() -> {
            // Password verification happens in Java so the submitted password
            // can be checked against the per-user salt stored in the hash.
            User user = repository.getUserByUsername(username.trim());
            if (user != null && PasswordHasher.verifyPassword(password.trim(), user.password)) {
                loginSuccess.postValue(new SingleEvent<>(user.username));
            } else {
                errorMessage.postValue(new SingleEvent<>(R.string.login_failed));
            }
        });
    }

    public void register(String username, String password) {
        int usernameError = InputValidator.validateUsername(username);
        if (usernameError != 0) {
            errorMessage.setValue(new SingleEvent<>(usernameError));
            return;
        }
        int passwordError = InputValidator.validatePassword(password);
        if (passwordError != 0) {
            errorMessage.setValue(new SingleEvent<>(passwordError));
            return;
        }

        DbExecutors.io().execute(() -> {
            // Only the salted hash is sent into the repository/database layer.
            String hashedPassword = PasswordHasher.hashPassword(password.trim());
            long result = repository.registerUser(username.trim(), hashedPassword);
            if (result != -1) {
                infoMessage.postValue(new SingleEvent<>(R.string.registration_success));
            } else {
                errorMessage.postValue(new SingleEvent<>(R.string.registration_username_exists));
            }
        });
    }
}
