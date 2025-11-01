package com.rocket.radar;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rocket.radar.databinding.NavBarBinding;
import com.rocket.radar.profile.ProfileModel;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;

    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;

    /**
     * This method runs the first time the activity is created and only then.
     * 
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle
     *                           contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.
     *                           <b><i>Note: Otherwise it is null.</i></b>
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAuth = FirebaseAuth.getInstance(); // Initiaize Firebase Auth

        super.onCreate(savedInstanceState);
        navBarBinding = NavBarBinding.inflate(getLayoutInflater());
        setContentView(navBarBinding.getRoot());

        // The repository is no longer needed here
        // eventRepository = new EventRepository();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(navBarBinding.bottomNavigationView, navController);
    }

    /**
     * This method is called when the activity becomes visible to the user.
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
     * Signs in the user anonymously with Firebase Auth.
     *
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

    private void handleUserSignIn(FirebaseUser user) {
        if (user == null) {
            Log.w(TAG, "No user signed in");
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = user.getUid();

        ProfileModel profile = new ProfileModel(uid, "Anonymous User", "","", null);

        db.collection("users")
                .document(uid)
                .set(profile)
                .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(uid).update("lastLogin", FieldValue.serverTimestamp());})
                .addOnFailureListener(e -> Log.w(TAG, "Error writing user document", e));
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (navBarBinding != null && navBarBinding.bottomNavigationView != null) {
            navBarBinding.bottomNavigationView.setVisibility(visibility);
        }
    }

}
