package com.rocket.radar.profile;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.rocket.radar.R;

public class AccountSettingsFragment extends Fragment {

    private MaterialButton backButton;
    private TextInputEditText usernameField, emailField, phoneNumberField;
    private ProfileViewModel profileViewModel;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_account_settings, container, false);
        backButton = view.findViewById(R.id.back_button);
        usernameField = view.findViewById(R.id.usernameField);
        emailField = view.findViewById(R.id.emailField);
        phoneNumberField = view.findViewById(R.id.phoneField);
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
        });
        return view;
    }
}
