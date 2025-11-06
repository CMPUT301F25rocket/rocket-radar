package com.rocket.radar.login;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.rocket.radar.MainActivity;
import com.rocket.radar.R;

/*
Basic fragment for showing login criteria information.
 */
public class LoginCriteriaFragment extends Fragment {
    private ImageButton button_back;
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        View view = inflater.inflate(R.layout.login_criteria, container, false);

        button_back = view.findViewById(R.id.button_back);
        button_back.setOnClickListener(v -> {
            NavHostFragment.findNavController(this).navigateUp();
        });
        return view;
    }
}
