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
import com.rocket.radar.profile.ProfileModel;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;

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
        mAuth = FirebaseAuth.getInstance();
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();
        ProfileModel profile = new ProfileModel(uid, "Anonymous User", "", "", null);
        db.collection("users")
                .document(uid)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(uid).update("lastLogin", FieldValue.serverTimestamp());
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error writing user document", e));
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }
}
