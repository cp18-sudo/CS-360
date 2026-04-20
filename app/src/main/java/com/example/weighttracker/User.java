package com.example.weighttracker;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for the users table. Username is the primary key.
 * The password field stores the salted hash produced by PasswordHasher,
 * not the user's plain-text password.
 */
@Entity(tableName = "users")
public class User {

    @PrimaryKey
    @NonNull
    public String username;

    /** Stored format is base64(salt):base64(hash). */
    public String password;

    public User(@NonNull String username, String password) {
        this.username = username;
        this.password = password;
    }
}
