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
    private String username;
    private String email;
    private String phoneNumber;
    private String deviceId;
    private ProfileViewModel profileViewModel;
    private FirebaseAuth mAuth;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        View view = inflater.inflate(R.layout.login_start_scanning, container, false);

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
                    getActivity().getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID
            );

            mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            String uid = user.getUid();
            profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
            ProfileModel defaultProfile = new ProfileModel(uid, username, email, phoneNumber, null, true, true, false);
            profileViewModel.updateProfile(defaultProfile);


            NavHostFragment.findNavController(this).navigate(R.id.action_login_start_scanning_to_event_list);
        });


        return view;
    }
}
