package com.rocket.radar.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.rocket.radar.R;

public class AccountSettingsFragment extends Fragment {

    private com.google.android.material.button.MaterialButton backButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_account_settings, container, false);
        backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });
        return view;
    }
}
