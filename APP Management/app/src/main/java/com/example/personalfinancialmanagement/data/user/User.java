package com.example.personalfinancialmanagement;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String username;

    @NonNull
    public String passwordHash; // stored hash

    public String fullName;
    public String email;
    public String phone;

    public User(long id, @NonNull String username, @NonNull String passwordHash, String fullName, String email, String phone) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    @Ignore
    public User(@NonNull String username, @NonNull String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = null;
        this.email = null;
        this.phone = null;
    }
}
