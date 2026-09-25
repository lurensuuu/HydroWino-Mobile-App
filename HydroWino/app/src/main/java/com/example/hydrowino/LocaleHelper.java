package com.example.hydrowino;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_NAME = "Settings";
    private static final String KEY_LANG = "My_Lang";

    // Save language
    public static void setLocale(Context context, String langCode) {
        saveLanguage(context, langCode);
        updateResources(context, langCode);
    }

    // Load language (used globally)
    public static Context onAttach(Context context) {
        String lang = getSavedLanguage(context);
        return updateResources(context, lang);
    }

    public static String getSavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "en"); // default English
    }

    private static void saveLanguage(Context context, String lang) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LANG, lang).apply();
    }

    private static Context updateResources(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration();
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}