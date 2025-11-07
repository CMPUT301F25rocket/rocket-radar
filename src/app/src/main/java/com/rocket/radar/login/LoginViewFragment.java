package com.rocket.radar.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;

/**
 Fragment for the main login view with options to start scanning or view criteria.
 */
public class LoginViewFragment extends Fragment {
    FirebaseAuth mAuth;
    private Button button_start_scanning;
    private Button button_criteria;

    /**
     * Inflates the layout for onCreateView and sets up criteria and start scanning buttons.
     */
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        View view = inflater.inflate(R.layout.login_main, container, false);
        button_start_scanning = view.findViewById(R.id.button_start_scanning);
        button_criteria = view.findViewById(R.id.button_criteria);
        button_start_scanning.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_login_main_to_login_start_scanning);

        });
        button_criteria.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigate(R.id.action_login_main_to_login_criteria);

        });
        return view;

    }

    /**
     * Initializes UI components after the view is created.
     */
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Initialize UI components
        super.onViewCreated(view, savedInstanceState);
    }
}
