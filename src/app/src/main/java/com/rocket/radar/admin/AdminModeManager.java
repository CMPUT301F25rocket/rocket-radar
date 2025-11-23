package com.rocket.radar.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.navigation.NavController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.rocket.radar.R;

// cite: the following script was adapted from ChatGPT and Claude,
// where ChatGPT recommended using shared preferences,
// and Claude recommended using live data and the singleton pattern.
// 2025-11-14
// cite: Claude recommended using a listener for user role changes
// 2025-11-23
/**
 * This class manages if an Admin is currently in Admin mode or not using shared preferences.
 * Shared preferences persists across logins.
 * Singleton pattern ensures the same LiveData instance is shared across the app.
 */
public class AdminModeManager {
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_ADMIN_MODE = "admin_mode";
    private static final String TAG = "AdminModeManager";

    private static AdminModeManager instance;
    private SharedPreferences prefs;
    private MutableLiveData<Boolean> adminModeLiveData;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private DocumentReference userDocListener;
    private NavController navController;
    private Context context;

    private AdminModeManager(Context context) {
        this.context = context.getApplicationContext();
        prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        adminModeLiveData = new MutableLiveData<>(isAdminModeOn());
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized AdminModeManager getInstance(Context context) {
        if (instance == null) {
            instance = new AdminModeManager(context);
        }
        return instance;
    }

    /**
     * Sets the NavController for navigation when admin status is lost.
     */
    public void setNavController(NavController navController) {
        this.navController = navController;
    }

    /**
     * Starts monitoring the current user's admin status in Firestore.
     * If their admin permissions are revoked, automatically exits admin mode and navigates away.
     */
    public void startMonitoringAdminStatus() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (uid == null) {
            Log.w(TAG, "Cannot monitor admin status: user not authenticated");
            return;
        }

        // Stop any previous listener
        stopMonitoringAdminStatus();

        // Listen to the user's profile document
        userDocListener = db.collection("users").document(uid);
        userDocListener.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Error monitoring admin status", error);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                boolean isAdmin = snapshot.getBoolean("isAdmin") != null && snapshot.getBoolean("isAdmin");

                // If user is no longer an admin but admin mode is on, disable it and navigate
                if (!isAdmin && isAdminModeOn()) {
                    //Log.w(TAG, "Admin permissions revoked for user: " + uid);
                    setAdminModeOn(false);
                    handleAdminPermissionsRevoked();
                }
            }
        });
    }

    /**
     * Stops monitoring the current user's admin status.
     */
    public void stopMonitoringAdminStatus() {
        if (userDocListener != null) {
            Log.d(TAG, "Stopped monitoring admin status");
        }
    }

    /**
     * Handles the case when admin permissions are revoked.
     * Navigates to EventViewFragment and shows a toast.
     */
    private void handleAdminPermissionsRevoked() {
        //Toast.makeText(context, "Your admin permissions were revoked.", Toast.LENGTH_LONG).show();

        if (navController != null) {
            navController.navigate(R.id.eventListFragment);
        } else {
            Log.e(TAG, "NavController not set. Cannot navigate.");
        }
    }

    public boolean isAdminModeOn() {
        return prefs.getBoolean(KEY_ADMIN_MODE, false);
    }

    public void setAdminModeOn(boolean on) {
        prefs.edit().putBoolean(KEY_ADMIN_MODE, on).apply();
        adminModeLiveData.setValue(on);
        Log.d(TAG, "Admin mode set to: " + on);
    }

    public LiveData<Boolean> getAdminModeLiveData() {
        return adminModeLiveData;
    }
}