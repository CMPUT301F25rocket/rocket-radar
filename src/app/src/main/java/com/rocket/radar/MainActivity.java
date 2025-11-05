package com.rocket.radar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rocket.radar.databinding.NavBarBinding;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private ProfileViewModel profileViewModel;

    // Location & Permission services
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isObserverInitialized = false;

    // Launcher for Notification Permissions (Android 13+)
    private final ActivityResultLauncher<String> requestNotificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted.");
                } else {
                    Log.w(TAG, "Notification permission denied by user.");
                }
            });

    // Launcher for Location Permissions
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Location permission granted by user.");
                    // Permission is granted. Now you can fetch the location.
                    fetchLastKnownLocation();
                } else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    Log.w(TAG, "Location permission denied by user.");
                    Toast.makeText(this, "Geolocation access is required for check-in features.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance();
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        super.onCreate(savedInstanceState);
        navBarBinding = NavBarBinding.inflate(getLayoutInflater());
        setContentView(navBarBinding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(navBarBinding.bottomNavigationView, navController);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Ask for standard permissions
        askNotificationPermission();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration Token: " + token);
                });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            signInAnonymously();
        else
            handleUserSignIn(currentUser);
    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInAnonymously:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        handleUserSignIn(user);
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        handleUserSignIn(null);
                    }
                });
    }

    private void handleUserSignIn(FirebaseUser user) {
        if (user == null) {
            Log.w(TAG, "No user signed in, cannot proceed.");
            return;
        }
        String uid = user.getUid();
        Log.d(TAG, "User signed in with UID: " + uid);
        profileViewModel.updateLastLogin(uid);

        profileViewModel.setUserIdForProfileListener(uid);

        if (!isObserverInitialized) {
            profileViewModel.getProfileLiveData().observe(this, new Observer<ProfileModel>() {
                @Override
                public void onChanged(ProfileModel profile) {
                    if (profile == null) {
                        Log.d(TAG, "First-time user detected. Creating default profile for UID: " + uid);
                        ProfileModel defaultProfile = new ProfileModel(uid, "Anonymous User", "", "", null, true, true, false);
                        profileViewModel.updateProfile(defaultProfile);
                        // No need to check permissions for a new profile, default is enabled.
                    } else {
                        Log.d(TAG, "Profile data received for user: " + profile.getUid());
                        // Profile exists, now check if we should ask for system location permission.
                        checkGeolocationPermission(profile);
                    }
                }
            });
            isObserverInitialized = true;
        }
    }

    /**
     * Checks if the user has enabled geolocation in their profile and if the app has system-level permission.
     * @param profile The user's profile model.
     */
    private void checkGeolocationPermission(ProfileModel profile) {
        // Step 1: Check if the user has consented in their app profile settings.
        if (profile.isGeolocationEnabled()) { // This call is now safe.
            Log.d(TAG, "User has geolocation enabled in their profile.");
            // Step 2: Check if the app has been granted the Android system permission.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted, request it from the user.
                Log.d(TAG, "System location permission not granted. Requesting it now.");
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                // Both profile setting and system permission are granted. We can fetch location.
                Log.d(TAG, "System location permission is already granted.");
                fetchLastKnownLocation();
            }
        } else {
            Log.d(TAG, "User has geolocation disabled in their profile. Not requesting system permission.");
        }
    }

    /**
     * Fetches the device's last known location.
     * This method must be called only after checking for ACCESS_FINE_LOCATION permission.
     */
    private void fetchLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Location found. We can use it.
                            // In a real scenario, you'd pass this to a ViewModel or save it during check-in.
                            Log.i(TAG, "Last known location: Lat: " + location.getLatitude() + ", Lon: " + location.getLongitude());
                            // For now, we will just show a toast.
                            Toast.makeText(this, "Location acquired!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Last known location is null. This can happen if GPS was recently turned off.
                            Log.w(TAG, "Last known location is null. A new location request might be needed.");
                        }
                    });
        }
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }
}
