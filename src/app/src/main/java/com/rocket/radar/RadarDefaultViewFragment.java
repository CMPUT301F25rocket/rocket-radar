package com.rocket.radar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.rocket.radar.loadingscreen.RadarView;

/**
 * A Fragment that displays the default radar scanning animation.
 *
 * <p>This fragment serves as a loading screen or a placeholder view while the
 * application initializes or fetches data. It manages the lifecycle of the
 * custom {@link RadarView} animation to ensure it starts when visible and stops
 * when the view is destroyed to prevent memory leaks.</p>
 *
 * <p><strong>Outstanding Issues:</strong>
 * <ul>
 *   <li>None currently known.</li>
 * </ul>
 * </p>
 */
public class RadarDefaultViewFragment extends Fragment {

    private RadarView radarView;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This implementation inflates the radar layout and hides the bottom navigation bar
     * in the hosting {@link MainActivity}.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate
     *                           any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's
     *                           UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     * @return Return the View for the fragment's UI.
     */
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

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     * has returned, but before any saved state has been restored in to the view.
     * This implementation locates the {@link RadarView} and starts its animation.
     *
     * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the view and start the animation
        radarView = view.findViewById(R.id.radar_animation_view);
        if (radarView != null) {
            radarView.startAnimation();
        }
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has
     * been detached from the fragment.
     * This implementation stops the radar animation to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up animation to prevent memory leaks
        if (radarView != null) {
            radarView.stopAnimation();
        }
    }
}
