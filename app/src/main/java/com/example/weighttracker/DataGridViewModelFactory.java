package com.example.weighttracker;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/** Factory that injects {@link WeightRepository} and userId into {@link DataGridViewModel}. */
public class DataGridViewModelFactory implements ViewModelProvider.Factory {

    private final WeightRepository repository;
    private final String userId;

    public DataGridViewModelFactory(WeightRepository repository, String userId) {
        this.repository = repository;
        this.userId = userId;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(DataGridViewModel.class)) {
            return (T) new DataGridViewModel(repository, userId);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
