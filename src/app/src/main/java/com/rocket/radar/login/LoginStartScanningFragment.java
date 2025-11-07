package com.rocket.radar.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

public class LoginStartScanningFragment extends Fragment {
    private Button button_continue;
    private static final String TAG = "MainActivity";

    private String username;
    private String email;
    private String phoneNumber;
    private String deviceId;
    private String uid;
    private ProfileViewModel profileViewModel;
    private FirebaseAuth mAuth;
    // A flag to ensure we only set up the observer once.
    private boolean isObserverInitialized = false;

    /**
     * Inflates the layout for this fragment has three input fields for email, email, and phone number.
     */
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity) requireActivity()).setBottomNavigationVisibility(View.GONE);
        View view = inflater.inflate(R.layout.login_start_scanning, container, false);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        mAuth = FirebaseAuth.getInstance();

        setupUI(view);

        return view;
    }

    /**
     * Sets up the UI elements and their event listeners.
     */
    private void setupUI(View view) {
        button_continue = view.findViewById(R.id.button_continue);
        TextInputLayout layout_username = view.findViewById(R.id.usernameLayout);
        TextInputLayout layout_email = view.findViewById(R.id.emailLayout);
        TextInputLayout layout_phoneNumber = view.findViewById(R.id.phoneNumberLayout);
        TextInputEditText input_username = view.findViewById(R.id.usernameInput);
        TextInputEditText input_email = view.findViewById(R.id.emailInput);
        TextInputEditText input_phoneNumber = view.findViewById(R.id.phoneNumberInput);

        button_continue.setOnClickListener(v -> {
            username = input_username.getText().toString();
            email = input_email.getText().toString();
            phoneNumber = input_phoneNumber.getText().toString();

            if (username.isEmpty()) {
                layout_username.setError("Username is required");
                return;
            }

            deviceId = android.provider.Settings.Secure.getString(
                    requireContext().getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID
            );

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getUid() != null) {
                String uid = user.getUid();
                ProfileModel defaultProfile = new ProfileModel(uid, username, email, phoneNumber, null, true, true, false);
                profileViewModel.updateProfile(defaultProfile);
            } else {
                // fallback: force Firebase to reload or sign in again
                mAuth.signInAnonymously().addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    ProfileModel defaultProfile = new ProfileModel(uid, username, email, phoneNumber, null, true, true, false);
                    profileViewModel.updateProfile(defaultProfile);
                });
            }

            NavHostFragment.findNavController(this).navigate(R.id.action_login_start_scanning_to_event_list);
        });
    }

    @Override
    /**
     * Fix for logging in after just deleting an account. Makes this fragment work again after redirect.
     */
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).setBottomNavigationVisibility(View.GONE);

        // Ensure Firebase user exists and update profile if needed
        updateOrCreateUser();
    }

    /**
     * Ensures that a Firebase user exists and updates or creates the user profile as needed.
     */
    private void updateOrCreateUser() {
        deviceId = android.provider.Settings.Secure.getString(
                requireContext().getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID
        );

        FirebaseUser user = mAuth.getCurrentUser();

        // CASE 1: user is null → sign in again
        if (user == null) {
            mAuth.signInAnonymously().addOnSuccessListener(result -> {
                FirebaseUser newUser = result.getUser();
                if (newUser != null) {
                    saveProfile(newUser.getUid());
                }
            });
            return;
        }

        // CASE 2: user exists but stale → force reload
        user.reload().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser refreshedUser = mAuth.getCurrentUser();
                if (refreshedUser != null && refreshedUser.getUid() != null) {
                    saveProfile(refreshedUser.getUid());
                }
            } else {
                // Reload failed, re-sign in just in case
                mAuth.signInAnonymously().addOnSuccessListener(result -> {
                    FirebaseUser newUser = result.getUser();
                    if (newUser != null) saveProfile(newUser.getUid());
                });
            }
        });
    }

    /**
     * Saves the user profile to the profileViewModel.
     */
    private void saveProfile(String uid) {
        ProfileModel defaultProfile = new ProfileModel(uid, username, email, phoneNumber, null, true, true, false);
        profileViewModel.updateProfile(defaultProfile);
    }
}