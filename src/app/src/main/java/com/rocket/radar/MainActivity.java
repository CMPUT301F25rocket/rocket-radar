// C:/Users/bwood/Cmput301/rocket-radar/src/app/src/main/java/com/rocket/radar/MainActivity.java
package com.rocket.radar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessaging;
import com.rocket.radar.databinding.NavBarBinding;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.events.EventViewFragment;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileRepository;
import com.rocket.radar.profile.ProfileViewModel;
import com.rocket.radar.qr.QRDialog;

/**
 Main activity that handles user authentication, navigation, and location services.
 */
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
                ProfileModel currentUser = profileViewModel.getProfileLiveData().getValue();
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted.");
                    if (currentUser != null) {
                        currentUser.setNotificationsEnabled(true);
                        profileViewModel.updateProfile(currentUser);
                    }

                } else {
                    Log.w(TAG, "Notification permission denied by user.");

                }
            });

    // Launcher for Location Permissions
    private final ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                ProfileModel currentUser = profileViewModel.getProfileLiveData().getValue();
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission has been granted (Precise or Coarse).");
                    fetchLastKnownLocation();
                } else {
                    Log.w(TAG, "Location permission was explicitly denied by user.");
                    Toast.makeText(this, "Geolocation access is required for some features.", Toast.LENGTH_SHORT).show();
                    if (currentUser != null) {
                        currentUser.setGeolocationEnabled(false);
                        profileViewModel.updateProfile(currentUser);
                    }
                }
            });

    @Override
    /**
     * Initializes the activity, sets up navigation, and requests necessary permissions.
     */
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
    /**
     * Requests notification permission for Android 13 and above.
     */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Handles user sign-in and intent actions when the activity starts.
     */
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            signInAnonymously();
        else
            handleUserSignIn(currentUser);

    }

    /**
     * This was devised by a moron. By default when a new intent is provided to an activity it is
     * ignored. This override ensures that it is not ignored.
     * @param intent The new intent that was given to the activity
     */
    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    /**
     * If an 
     */
    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action == null) return;
        // TODO: This is begging to be refactored.
        ProfileModel currentProfile = profileViewModel.getProfileLiveData().getValue();
        if (action.equals("android.intent.action.VIEW")) {
            Uri uri = intent.getData();
            String id = uri.getQueryParameter("eventId");
            if (id == null) {
                MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this);
                dialog.setTitle("Scan Error");
                dialog.setMessage("The link did not provide and event it");
                dialog.setPositiveButton("Ok", null);
                dialog.show();
                return;
            }
            EventRepository eventRepository = new EventRepository();
            eventRepository.getEvent(id)
                    .addOnSuccessListener(documentSnapshot -> {
                        Event event = documentSnapshot.toObject(Event.class);
                        if (event == null) {
                            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this);
                            dialog.setTitle("Fetch Error");
                            dialog.setMessage("This event does not exist.");
                            dialog.setPositiveButton("Ok", null);
                            dialog.show();
                        }

                        // Figure out if we are the organizer for this event
                        boolean isOrganizer = false;
                        ProfileModel profileModel = profileViewModel.getProfileLiveData().getValue();
                        if (profileModel != null) {
                            for (var myEventId : profileModel.getOnMyEventIds()) {
                                if (myEventId.equals(id)) {
                                    isOrganizer = true;
                                    break;
                                }
                            }
                        }

                        EventViewFragment eventViewFragment = EventViewFragment.newInstance(event, isOrganizer);
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.nav_host_fragment, eventViewFragment)
                                .addToBackStack(EventViewFragment.TAG)
                                .commit();
                    })
                    .addOnFailureListener(why -> {
                        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(MainActivity.this);
                        dialog.setTitle("Fetch error");
                        dialog.setMessage("Failed to fetch the event with id `" + id + "`");
                        dialog.setPositiveButton("Ok", null);
                        dialog.show();
                    });
        } else if (action.equals(getString(R.string.intent_action_show_qr))) {
            String eventId = intent.getStringExtra("eventId");
            QRDialog qrDialog = new QRDialog(getApplicationContext(), eventId);
            if (currentProfile != null) {
                currentProfile.addOnMyEventId(eventId);
                repo.addEventIdToMyEvent(currentProfile.getUid(), eventId, new ProfileRepository.WriteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Event ID added to Firestore for user: " + currentProfile.getUid());
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Failed to add event ID to Firestore", e);
                    }
                });
            }
            else {
                Log.d(TAG, "Profile is null");
                profileViewModel.getProfileLiveData().observe(this, new Observer<ProfileModel>() {
                    @Override
                    public void onChanged(ProfileModel profile) {
                        if (profile != null) {
                            profile.addOnMyEventId(eventId);
                            repo.addEventIdToMyEvent(profile.getUid(), eventId, new ProfileRepository.WriteCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Event ID added to Firestore after profile load");
                                }

                                @Override
                                public void onError(Exception e) {
                                    Log.e(TAG, "Failed to add event ID after profile load", e);
                                }
                            });
                        }
                    }
                });

            }
            qrDialog.show(getSupportFragmentManager(), QRDialog.TAG);
        } else  {
            Log.e(TAG, "Unrecognized action for MainActivity: " + action);
        }
    }

    /**
     * Signs in the user anonymously using Firebase Authentication.
     */
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

    /**
     * Handles actions to be taken when a user signs in.
     * @param user The signed-in Firebase user.
     */
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

        // WARN: This is new. May cause a bug. But without it caused a bug.
        // Activities started that went back to main activity without starting a new main activity
        // would crash the fragments that made them. The slop machine told me so.
        if (navController.getCurrentDestination() != null
            && navController.getCurrentDestination().getId() == R.id.radarDefaultViewFragment) {
            navController.navigate(R.id.action_returning_user_event_list);
        }

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

    /**
     * Checks if geolocation permission is needed based on user profile settings and requests it if necessary.
     * @param profile The user's profile model.
     */
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

    /**
     * Fetches the last known location of the user and updates their profile in Firestore.
     */
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

    /**
     * Sets the visibility of the bottom navigation bar.
     * @param visibility View.VISIBLE, View.INVISIBLE, or View.GONE
     */
    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }
}
