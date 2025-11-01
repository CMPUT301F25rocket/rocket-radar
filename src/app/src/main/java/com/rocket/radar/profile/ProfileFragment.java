package com.rocket.radar.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.rocket.radar.R;

public class ProfileFragment extends Fragment {

    private ImageButton accountSettingsButton;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);
        accountSettingsButton = view.findViewById(R.id.account_settings_button);
        accountSettingsButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profile_to_accountSettings);
        });
        return view;
    }
}