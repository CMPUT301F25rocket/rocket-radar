package com.rocket.radar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer; // Import Observer
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
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
    private EventRepository eventRepository;

    // A flag to ensure we only set up the observer once.
    private boolean isObserverInitialized = false;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d(TAG, "Notification permission granted.");
                } else {
                    Log.w(TAG, "Notification permission denied by user.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance(); // Initiaize Firebase Auth
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        super.onCreate(savedInstanceState);
        navBarBinding = NavBarBinding.inflate(getLayoutInflater());
        setContentView(navBarBinding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(navBarBinding.bottomNavigationView, navController);

        askNotificationPermission();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    String msg = "FCM Registration Token: " + token;
                    Log.d(TAG, msg);
                    Toast.makeText(MainActivity.this, "Token received! Check logs.", Toast.LENGTH_SHORT).show();
                });
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
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

        // **FIX**: Move the updateLastLogin call here. It now runs only once upon sign-in.
        Log.d(TAG, "User signed in with UID: " + uid + ". Updating last login time.");
        profileViewModel.updateLastLogin(uid);

        // Explicitly tell the ViewModel to start listening for this user's profile.
        profileViewModel.setUserIdForProfileListener(uid);

        // **FIX**: Ensure the observer is only set up once to prevent re-attaching it on every onStart().
        if (!isObserverInitialized) {
            profileViewModel.getProfileLiveData().observe(this, new Observer<ProfileModel>() {
                @Override
                public void onChanged(ProfileModel profile) {
                    // The observer's only job now is to create a profile if one doesn't exist.
                    if (profile == null) {
                        // The snapshot listener returned null, meaning this is a first-time user.
                        Log.d(TAG, "First-time user detected. Creating default profile for UID: " + uid);
                        ProfileModel defaultProfile = new ProfileModel(uid, "Anonymous User", "", "", null, true, true, false);
                        profileViewModel.updateProfile(defaultProfile);
                    } else {
                        // The profile exists. We don't need to do anything here anymore.
                        Log.d(TAG, "Profile data received for user: " + profile.getUid());
                    }
                }
            });
            isObserverInitialized = true;
        }
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }
}
