package com.rocket.radar.admin;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

// cite: the following was first adapted from ChatGPT and then Claude, on how to manage UI specific
// state for the admin mode. ChatGPT recommended using shared preferences and Claude later
// recommended live data. 2025-11-14
/**
 * This class manages if an Admin is currently in Admin mode or not using shared preferences.
 * Shared preferences persists across logins.
 */
public class AdminModeManager {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ADMIN_MODE = "admin_mode";

    private SharedPreferences prefs;
    private MutableLiveData<Boolean> adminModeLiveData;

    public AdminModeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        adminModeLiveData = new MutableLiveData<>(isAdminModeOn());
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