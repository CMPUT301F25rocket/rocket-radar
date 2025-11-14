package com.rocket.radar.admin;

import android.content.Context;
import android.content.SharedPreferences;

//cite: the following class is adapted from ChatGPT,
// "im going to change isAdmin to adminModeOn which holds state for if the admin is currently in admin mode or not. unless you think here is an easier way to hold that state"
// 2025-11-14
/**
 * This class manages if an Admin is currently in Admin mode or not using shared preferences.
 * Shared preferences persists across logins.
 */
public class AdminModeManager {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ADMIN_MODE = "admin_mode";

    private SharedPreferences prefs;

    public AdminModeManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isAdminModeOn() {
        return prefs.getBoolean(KEY_ADMIN_MODE, false);
    }

    public void setAdminModeOn(boolean on) {
        prefs.edit().putBoolean(KEY_ADMIN_MODE, on).apply();
    }
}

