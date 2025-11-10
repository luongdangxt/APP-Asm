package com.example.personalfinancialmanagement;

import android.content.Context;

class UserRepository {
    private final UserDao userDao;

    UserRepository(Context context) {
        this.userDao = AppDatabase.getInstance(context).userDao();
    }

    long register(String username, String password) {
        String hash = PasswordHasher.sha256(password);
        if (username == null || username.trim().isEmpty()) return -1L;
        User existing = userDao.findByUsername(username);
        if (existing != null) {
            return -1L;
        }
        return userDao.insert(new User(username, hash));
    }

    User login(String username, String password) {
        if (username == null || username.trim().isEmpty()) return null;
        User user = userDao.findByUsername(username);
        if (user == null) return null;
        String hash = PasswordHasher.sha256(password);
        if (hash.equals(user.passwordHash)) return user;
        return null;
    }
}
