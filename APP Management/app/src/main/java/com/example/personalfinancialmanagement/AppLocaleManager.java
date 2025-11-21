package com.example.personalfinancialmanagement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Centralizes locale changes so the user can switch languages at runtime.
 */
public final class AppLocaleManager {
    public static final String CODE_SYSTEM = "system";
    public static final String CODE_ENGLISH = "en";
    public static final String CODE_VIETNAMESE = "vi";

    private AppLocaleManager() { }

    public static void applySavedLocale(@NonNull Context context) {
        SettingsRepository prefs = new SettingsRepository(context.getApplicationContext());
        applyLocale(prefs.languageCode());
    }

    public static void applyLocale(String code) {
        String safeCode = normalize(code);
        LocaleListCompat locales;
        if (CODE_SYSTEM.equals(safeCode)) {
            locales = LocaleListCompat.getEmptyLocaleList();
        } else {
            locales = LocaleListCompat.forLanguageTags(safeCode);
        }
        AppCompatDelegate.setApplicationLocales(locales);
    }

    public static CharSequence languageLabel(@NonNull Context context, String code) {
        String normalized = normalize(code);
        if (CODE_VIETNAMESE.equals(normalized)) {
            return context.getString(R.string.settings_language_vietnamese);
        }
        if (CODE_ENGLISH.equals(normalized)) {
            return context.getString(R.string.settings_language_english);
        }
        return context.getString(R.string.settings_language_system);
    }

    private static String normalize(String code) {
        if (code == null) return CODE_SYSTEM;
        switch (code) {
            case CODE_ENGLISH:
            case CODE_VIETNAMESE:
                return code;
            default:
                return CODE_SYSTEM;
        }
    }
}
