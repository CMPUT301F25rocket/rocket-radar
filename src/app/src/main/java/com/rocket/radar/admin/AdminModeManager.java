package com.rocket.radar.admin;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// cite: the following script was adapted from ChatGPT and Claude,
// where ChatGPT recommended using shared preferences,
// and Claude recommended using live data and the singleton pattern.
// 2025-11-14
/**
 * This class manages if an Admin is currently in Admin mode or not using shared preferences.
 * Shared preferences persists across logins.
 * Singleton pattern ensures the same LiveData instance is shared across the app.
 */
public class AdminModeManager {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ADMIN_MODE = "admin_mode";

    private static AdminModeManager instance;
    private SharedPreferences prefs;
    private MutableLiveData<Boolean> adminModeLiveData;

    private AdminModeManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        adminModeLiveData = new MutableLiveData<>(isAdminModeOn());
    }

    public static synchronized AdminModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdminModeManager(context);
        }
        return instance;
    }

    public boolean isAdminModeOn() {
        return prefs.getBoolean(KEY_ADMIN_MODE, false);
    }

    public void setAdminModeOn(boolean on) {
        prefs.edit().putBoolean(KEY_ADMIN_MODE, on).apply();
        adminModeLiveData.setValue(on);
    }

    public LiveData<Boolean> getAdminModeLiveData() {
        return adminModeLiveData;
    }
}