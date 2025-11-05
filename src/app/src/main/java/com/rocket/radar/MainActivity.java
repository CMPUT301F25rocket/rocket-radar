package com.rocket.radar;

import android.Manifest;
import android.content.Intent;
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
import com.rocket.radar.qr.QRDialog;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private ProfileViewModel profileViewModel;

    // Location & Permission services
    private FusedLocationProviderClient fusedLocationClient;
    private NavController navController;

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
    // Launcher for Location Permissions
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // After the dialog closes, re-check the actual current permission status.
                // This correctly handles the "Only this time" case for both Precise and Approximate.

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    // This block will now execute if the user grants ANY location permission.
                    Log.d(TAG, "Location permission has been granted (Precise or Coarse).");
                    fetchLastKnownLocation();
                } else {
                    // This block now only runs for an explicit "Deny".
                    Log.w(TAG, "Location permission was explicitly denied by user.");
                    Toast.makeText(this, "Geolocation access is required for some features.", Toast.LENGTH_SHORT).show();
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
        navController = navHostFragment.getNavController();
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

        Intent intent = getIntent();
        String action = intent.getAction();
        if (action == null) return;
        if (action.equals(getString(R.string.intent_action_view_event))) {
            // TODO
            // navController.navigate();
        } else if (action.equals(getString(R.string.intent_action_show_qr))) {
            String eventId = intent.getStringExtra("eventId");
            QRDialog qrDialog = new QRDialog(eventId);
            qrDialog.show(getSupportFragmentManager(), QRDialog.TAG);
        } else  {
            Log.e(TAG, "Unrecognized action for MainActivity: " + action);
        }
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

    /**     * Fetches the device's last known location.
     * This method must be called only after checking for location permission.
     */
    private void fetchLastKnownLocation() {
        // Check if either FINE or COARSE location permission is granted.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // This is the call that gets the location.
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        // The 'location' object is what you want to print.
                        if (location != null) {
                            // Location was found successfully.
                            // The following line will print the Latitude and Longitude to your Logcat.
                            Log.i(TAG, "USER LOCATION: Latitude=" + location.getLatitude() + ", Longitude=" + location.getLongitude());

                            // This toast message also confirms it's working.
                            Toast.makeText(this, "Location acquired!", Toast.LENGTH_SHORT).show();

                            // TODO: When this is called from `triggerLocationUpdateAndSave`, the logic inside that
                            //  method's listener will execute, saving the location to Firestore.
                        } else {
                            // This can happen if location was recently turned off or on a new emulator.
                            Log.w(TAG, "Last known location is null. A new location request might be needed or location is disabled on the device.");
                        }
                    });
        } else {
            // This is a safeguard in case the method is called without permission.
            Log.e(TAG, "fetchLastKnownLocation called without any location permissions granted.");
        }
    }

    /**
     * PUBLIC method that can be called from any fragment to "freeze" the user's current location for an event.
     * This will be called from EventViewFragment when the "Join Waitlist" button is clicked.
     *
     * @param eventId The ID of the event the user is signing up for.
     */
    public void triggerLocationUpdateAndSave(String eventId) {
        // First, ensure we have permission. If not, the normal permission flow will be triggered.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Location permission not granted. Cannot save location for event.");
            // Optionally, you could trigger the permission request again here.
            // requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        // We have permission, so get the location.
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        Log.d(TAG, "Location acquired for event signup: " + eventId);

                        // TODO: Step 1 - Create a new Firestore document.
                        //  This would likely be in a new sub-collection, e.g., /events/{eventId}/checkins/{userId}

                        // TODO: Step 2 - Create a map or a data object containing the location data.
                        //  e.g., Map<String, Object> checkInData = new HashMap<>();
                        //  checkInData.put("userId", [current_user_id]);
                        //  checkInData.put("signupLocation", new GeoPoint(location.getLatitude(), location.getLongitude()));
                        //  checkInData.put("signupTimestamp", FieldValue.serverTimestamp());

                        // TODO: Step 3 - Save the data to Firestore.
                        //  e.g., FirebaseFirestore.getInstance().collection("events").document(eventId)
                        //      .collection("checkins").document([current_user_id]).set(checkInData);

                        Toast.makeText(this, "Your location has been saved for this event!", Toast.LENGTH_LONG).show();

                    } else {
                        Log.w(TAG, "Could not get location to save for event signup.");
                        Toast.makeText(this, "Could not determine your location. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }
}
