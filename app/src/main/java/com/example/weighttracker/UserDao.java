package com.example.weighttracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/** Data-access object for user accounts. */
@Dao
public interface UserDao {

    /**
     * Inserts a user; returns -1 if the username already exists
     * (IGNORE conflict strategy).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertUser(User user);

    /**
     * Fetch a user by username so LoginViewModel can verify the submitted
     * password against the stored salted hash.
     */
    @Query("SELECT * FROM users WHERE username = :username")
    User getUserByUsername(String username);
}
