// C:/Users/bwood/Cmput301/rocket-radar/src/app/src/main/java/com/rocket/radar/MainActivity.java
package com.rocket.radar;

import android.Manifest;import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rocket.radar.databinding.NavBarBinding;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileRepository;
import com.rocket.radar.profile.ProfileViewModel;
import com.rocket.radar.qr.QRDialog;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private ProfileViewModel profileViewModel;

    // Location & Permission services
    private FusedLocationProviderClient fusedLocationClient;
    private NavController navController;
    private ProfileRepository repo = new ProfileRepository();

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
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission has been granted (Precise or Coarse).");
                    fetchLastKnownLocation();
                } else {
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
        } else if (action.equals(getString(R.string.intent_action_show_qr))) {
            String eventId = intent.getStringExtra("eventId");
            QRDialog qrDialog = new QRDialog(getApplicationContext(), eventId);
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
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        if (user == null) {
            Log.w(TAG, "No user signed in, cannot proceed.");
            return;
        }
        String uid = user.getUid();

        Log.d(TAG, "User signed in with UID: " + uid + ". Updating last login time.");
        navController.navigate(R.id.action_returning_user_event_list);
        profileViewModel.updateLastLogin(uid);

        profileViewModel.setUserIdForProfileListener(uid);

        if (!isObserverInitialized) {
            profileViewModel.getProfileLiveData().observe(this, new Observer<ProfileModel>() {
                @Override
                public void onChanged(ProfileModel profile) {
                    if (profile == null) {
                        navController.navigate(R.id.action_first_time_login_main);
                        Log.d(TAG, "First-time user detected. Creating default profile for UID: " + uid);
                    } else {
                        Log.d(TAG, "Profile data received for user: " + profile.getUid());
                        checkGeolocationPermission(profile);
                    }
                }
            });
            isObserverInitialized = true;
        }
    }

    private void checkGeolocationPermission(ProfileModel profile) {
        if (profile.isGeolocationEnabled()) {
            Log.d(TAG, "User has geolocation enabled in their profile.");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "System location permission not granted. Requesting it now.");
                requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            } else {
                Log.d(TAG, "System location permission is already granted.");
                fetchLastKnownLocation();
            }
        } else {
            Log.d(TAG, "User has geolocation disabled in their profile. Not requesting system permission.");
        }
    }

    private void fetchLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            Log.i(TAG, "USER LOCATION: Latitude=" + location.getLatitude() + ", Longitude=" + location.getLongitude());
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            if (currentUser != null) {
                                GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                                // Use the repository to update the user's profile
                                repo.updateUserProfileLocation(currentUser.getUid(), geoPoint);
                            }
                            Toast.makeText(this, "Location acquired and updated!", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Last known location is null.");
                        }
                    });
        } else {
            Log.e(TAG, "fetchLastKnownLocation called without any location permissions granted.");
        }
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }
}
