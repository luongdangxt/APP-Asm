package com.example.personalfinancialmanagement;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Handles persisting login sessions when the user opts to remember their account.
 */
public class SessionManager {
    private static final String PREF_NAME = "pfm_session";
    private static final String KEY_REMEMBER = "remember_login";
    private static final String KEY_USER_ID = "remembered_user_id";
    private static final String KEY_USERNAME = "remembered_username";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void updateRememberedUser(boolean remember, long userId, String username) {
        SharedPreferences.Editor editor = prefs.edit();
        if (remember && userId > 0) {
            editor.putBoolean(KEY_REMEMBER, true);
            editor.putLong(KEY_USER_ID, userId);
            if (username != null) {
                editor.putString(KEY_USERNAME, username);
            }
        } else {
            editor.remove(KEY_REMEMBER);
            editor.remove(KEY_USER_ID);
            editor.remove(KEY_USERNAME);
        }
        editor.apply();
    }

    public boolean shouldAutoLogin() {
        return prefs.getBoolean(KEY_REMEMBER, false) && prefs.getLong(KEY_USER_ID, -1L) > 0;
    }

    public long getRememberedUserId() {
        return prefs.getLong(KEY_USER_ID, -1L);
    }

    public String getRememberedUsername() {
        return prefs.getString(KEY_USERNAME, "");
    }

    public boolean isRememberEnabled() {
        return prefs.getBoolean(KEY_REMEMBER, false);
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
