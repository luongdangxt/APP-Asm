package com.example.personalfinancialmanagement;

import android.content.Context;
import android.content.SharedPreferences;

class SettingsRepository {
    private static final String PREFS = "settings_prefs";
    private static final String KEY_NOTIF = "notif_enabled";
    private static final String KEY_BUDGET_ALERT = "budget_alert_enabled";
    private static final String KEY_THEME = "theme_mode"; // 0 system, 1 light, 2 dark

    private final SharedPreferences sp;

    SettingsRepository(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    boolean notificationsEnabled() { return sp.getBoolean(KEY_NOTIF, true); }
    void setNotificationsEnabled(boolean v) { sp.edit().putBoolean(KEY_NOTIF, v).apply(); }

    boolean budgetAlertsEnabled() { return sp.getBoolean(KEY_BUDGET_ALERT, true); }
    void setBudgetAlertsEnabled(boolean v) { sp.edit().putBoolean(KEY_BUDGET_ALERT, v).apply(); }

    int themeMode() { return sp.getInt(KEY_THEME, 0); }
    void setThemeMode(int mode) { sp.edit().putInt(KEY_THEME, mode).apply(); }
}
