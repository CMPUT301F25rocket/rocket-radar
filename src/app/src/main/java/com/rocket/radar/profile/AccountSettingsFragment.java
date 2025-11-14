package com.rocket.radar.profile;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rocket.radar.R;
import com.rocket.radar.admin.AdminModeManager;

// cite: toast code for save button based on https://developer.android.com/guide/topics/ui/notifiers/toasts, accessed: November 3, 2025

/**
 * This fragment is where the user can edit their settings.
 * This includes personal information, notification and geolocation preferences.
 * Input is validated and modifies the ProfileViewModel.
 */
public class AccountSettingsFragment extends Fragment {

    private MaterialButton backButton, saveButton, deleteButton, adminButton;
    private TextInputEditText usernameField, emailField, phoneNumberField;
    private MaterialSwitch notificationsEnabled, geolocationEnabled;
    private ProfileViewModel profileViewModel;
    private AdminModeManager adminModeManager;

    private String uid;

    /**
     * Inflates the fragment layout, initializes UI,
     * adds listeners for buttons, observes changes in ProfileViewModel
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the fragment root view
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_account_settings, container, false);
        backButton = view.findViewById(R.id.back_button);
        saveButton = view.findViewById(R.id.save_button);
        deleteButton = view.findViewById(R.id.delete_button);
        adminButton = view.findViewById(R.id.admin_button);
        usernameField = view.findViewById(R.id.usernameField);
        emailField = view.findViewById(R.id.emailField);
        phoneNumberField = view.findViewById(R.id.phoneField);
        notificationsEnabled = view.findViewById(R.id.notification_switch);
        geolocationEnabled = view.findViewById(R.id.geolocation_switch);
        adminModeManager = new AdminModeManager(requireContext());
        backButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });

        // this is for when the user clicks off a text input, it stops being focused
        // it's not great and doesn't work for clicking everwhere, only in certain spots
        view.setOnTouchListener((v, event) -> {
            usernameField.clearFocus();
            emailField.clearFocus();
            phoneNumberField.clearFocus();
            return false;
        });

        // cite: The following 11 lines are from Claude "If I change the manager to use live data, how should I refactor Account Settings?", 2025-11-14
        adminModeManager.getAdminModeLiveData().observe(getViewLifecycleOwner(), isAdminMode -> {
            adminButton.setText(isAdminMode ? "SWITCH BACK TO NORMAL" : "SWITCH TO ADMINISTRATOR");
        });

        adminButton.setOnClickListener(v -> {
            boolean newState = !adminModeManager.isAdminModeOn();
            adminModeManager.setAdminModeOn(newState);
            Toast.makeText(getContext(),
                    "Admin mode " + (newState ? "enabled" : "disabled"),
                    Toast.LENGTH_SHORT).show();
        });

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile == null) {
                usernameField.setText("");
                emailField.setText("");
                phoneNumberField.setText("");
                notificationsEnabled.setChecked(false);
                geolocationEnabled.setChecked(false);
                adminButton.setVisibility(View.GONE);
                uid = null;
                return;
            }
            usernameField.setText(profile.getName());
            emailField.setText(profile.getEmail());
            phoneNumberField.setText(profile.getPhoneNumber());
            uid = profile.getUid();
            // cite: the following two lines are from ChatGPT, "What is the safest way to check for a True Boolean in Java?", accessed: October 27, 2025
            notificationsEnabled.setChecked(Boolean.TRUE.equals(profile.isNotificationsEnabled()));
            geolocationEnabled.setChecked(Boolean.TRUE.equals(profile.isGeolocationEnabled()));

            if (profile.getRole() == ProfileModel.UserRole.ADMIN) {
                adminButton.setVisibility(View.VISIBLE);
            } else {
                adminButton.setVisibility(View.GONE);
            }
        });

        saveButton.setOnClickListener( v -> {
            boolean isValid = true;
            String username = InputValidator.cleanText(usernameField);
            String email = InputValidator.cleanText(emailField);
            String phone = InputValidator.cleanText(phoneNumberField);

            if (!InputValidator.isValidEmail(email)) {
                emailField.setError("Invalid email");
                isValid = false;
            } else {
                emailField.setError(null);
            }

            if (!InputValidator.isValidPhone(phone)) {
                phoneNumberField.setError("Invalid phone number");
                isValid = false;
            } else {
                phoneNumberField.setError(null);
            }

            if (username.isEmpty()) {
                usernameField.setError("Name cannot be empty!");
                isValid = false;
            } else {
                usernameField.setError(null);
            }

            if (!isValid) {
                Toast.makeText(getContext(), "Please fix the errors above", Toast.LENGTH_SHORT).show();
                return;
            }

            ProfileModel profile = profileViewModel.getProfileLiveData().getValue();
            if (profile != null) {
                profile.setName(username);
                profile.setEmail(email);
                profile.setPhoneNumber(phone);
                profile.setNotificationsEnabled(notificationsEnabled.isChecked());
                profile.setGeolocationEnabled(geolocationEnabled.isChecked());
                // profile.setRole(ProfileModel.UserRole.ADMIN); // debug line to make you admin
                profileViewModel.updateProfile(profile);
            }
            Toast saveToast = Toast.makeText(this.getContext(), "Account settings saved!", Toast.LENGTH_SHORT);
            saveToast.show();

            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_account_settings_to_profile);
        });
        deleteButton.setOnClickListener( v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete your account?")
                    .setMessage("This will permanently remove your profile and all associated data. This action cannot be undone.")
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setPositiveButton("Delete Account", (dialog, which) -> {
                        confirmAccountDelete();
                    })
                    .show();
        });

        return view;
    }

    /**
     * This function handles the logic for when the user presses "Delete Account" on the dialog.
     * It calls deleteProfile on the ProfileViewModel, and then if that succeeds, it navigates back to the login.
     * A toast is also displayed for success and error.
     */
    public void confirmAccountDelete() {
        ProfileModel profile = profileViewModel.getProfileLiveData().getValue();
        profileViewModel.deleteProfile(profile);

        profileViewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null && success) {
                Toast.makeText(getContext(), "Account deleted", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_account_settings_to_login);
            } else if (success != null) {
                Toast.makeText(getContext(), "Failed to delete account", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
