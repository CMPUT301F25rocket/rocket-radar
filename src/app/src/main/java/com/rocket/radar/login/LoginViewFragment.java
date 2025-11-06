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

import com.rocket.radar.MainActivity;
import com.rocket.radar.R;


public class LoginViewFragment extends Fragment {

    private Button button_start_scanning;
    private Button button_criteria;

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

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Initialize UI components
        super.onViewCreated(view, savedInstanceState);
    }
}
