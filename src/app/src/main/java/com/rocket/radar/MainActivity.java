package com.rocket.radar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast; // Import Toast

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull; // Import NonNull
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener; // Import OnCompleteListener
import com.google.android.gms.tasks.Task; // Import Task
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging; // Import FirebaseMessaging
import com.rocket.radar.databinding.NavBarBinding;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private ProfileViewModel profileViewModel;

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

        // --- START: ADDED CODE FROM DOCUMENTATION ---
        // This will proactively fetch the token and log it.
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast the token
                        String msg = "FCM Registration Token: " + token;
                        Log.d(TAG, msg);
                        Toast.makeText(MainActivity.this, "Token received! Check logs.", Toast.LENGTH_SHORT).show();
                    }
                });
        // --- END: ADDED CODE FROM DOCUMENTATION ---
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
            Log.w(TAG, "No user signed in");
            return;
        }
        String uid = user.getUid();

        // Use a temporary, one-time observer to handle the initial sign-in logic
        profileViewModel.getProfileLiveData().observeForever(new androidx.lifecycle.Observer<ProfileModel>() {
            @Override
            public void onChanged(ProfileModel profile) {
                // Once we get a non-null profile, we perform our logic and then stop listening.
                if (profile != null) {
                    // If it's a known user, update their last login time.
                    profileViewModel.updateLastLogin(profile.getUid());
                    Log.d(TAG, "Profile loaded successfully: " + profile.getUid());

                    // IMPORTANT: Remove the observer to prevent the infinite loop.
                    profileViewModel.getProfileLiveData().removeObserver(this);
                } else {
                    // This 'else' block will likely run briefly while the profile is first being fetched.
                    // We can check if it's the very first time the user is ever seen.
                    // The logic to create a new profile if it's truly null after a fetch is handled below.
                }
            }
        });

        // Initial fetch to get the process started.
        profileViewModel.getProfile(uid);

        // Separately, handle the case of a brand new user.
        // This is a slightly different way to check for a new user.
        FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().exists()) {
                // The document does not exist, this is a first-time user.
                Log.d(TAG, "First-time user detected. Creating default profile.");
                ProfileModel defaultProfile = new ProfileModel(uid, "Anonymous User", "", "", null);
                profileViewModel.updateProfile(defaultProfile);
            }
        });
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }
}
