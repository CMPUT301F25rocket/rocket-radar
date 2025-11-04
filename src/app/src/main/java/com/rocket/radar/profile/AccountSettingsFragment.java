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
import com.rocket.radar.R;

public class AccountSettingsFragment extends Fragment {

    private MaterialButton backButton, saveButton;
    private TextInputEditText usernameField, emailField, phoneNumberField;
    private MaterialSwitch notificationsEnabled, geolocationEnabled;
    private ProfileViewModel profileViewModel;

    private String uid;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_account_settings, container, false);
        backButton = view.findViewById(R.id.back_button);
        saveButton = view.findViewById(R.id.save_button);
        usernameField = view.findViewById(R.id.usernameField);
        emailField = view.findViewById(R.id.emailField);
        phoneNumberField = view.findViewById(R.id.phoneField);
        notificationsEnabled = view.findViewById(R.id.notification_switch);
        geolocationEnabled = view.findViewById(R.id.geolocation_switch);
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

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            usernameField.setText(profile.getName());
            emailField.setText(profile.getEmail());
            phoneNumberField.setText(profile.getPhoneNumber());
            uid = profile.getUid();
            notificationsEnabled.setChecked(profile.isNotificationsEnabled());
            geolocationEnabled.setChecked(profile.isGeolocationEnabled());
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

            if (!isValid) {
                Toast.makeText(getContext(), "Please fix the errors above", Toast.LENGTH_SHORT).show();
                return;
            }

            ProfileModel newProfile = new ProfileModel(
                    uid,
                    username,
                    email,
                    phone,
                    null,
                    notificationsEnabled.isChecked(),
                    geolocationEnabled.isChecked(),
                    false
            );
            profileViewModel.updateProfile(newProfile);
            Toast saveToast = Toast.makeText(this.getContext(), "Account settings saved!", Toast.LENGTH_SHORT);
            saveToast.show();
        });
        return view;
    }
}
