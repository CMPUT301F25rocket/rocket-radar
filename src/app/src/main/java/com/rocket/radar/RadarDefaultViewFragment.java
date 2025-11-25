package com.rocket.radar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.rocket.radar.loadingscreen.RadarView;

public class RadarDefaultViewFragment extends Fragment {

    private RadarView radarView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.radar_default_view, container, false);

        // Hide bottom nav since we are loading
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the view and start the animation
        radarView = view.findViewById(R.id.radar_animation_view);
        if (radarView != null) {
            radarView.startAnimation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up animation to prevent memory leaks
        if (radarView != null) {
            radarView.stopAnimation();
        }
    }
}
