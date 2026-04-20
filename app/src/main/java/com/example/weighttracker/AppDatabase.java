package com.example.weighttracker;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database singleton. Thread-safe initialization via double-checked
 * locking with a volatile instance field. Three entities: User,
 * WeightEntry, and Goal.
 *
 * For this capstone demo I use destructive migration because the original
 * schema did not have user ownership on weight rows. In a production app,
 * this would be replaced with an explicit migration that preserves data.
 */
@Database(entities = {User.class, WeightEntry.class, Goal.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract WeightDao weightDao();
    public abstract GoalDao goalDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "weight_tracker_db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
