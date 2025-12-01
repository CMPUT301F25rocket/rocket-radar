package com.rocket.radar.loadingscreen;

import android.view.View;
import android.widget.TextView;
import com.rocket.radar.R;
import com.rocket.radar.loadingscreen.RadarView;

/**
 * Manages the display of a loading overlay with a radar animation.
 * This class provides a simple API to show and hide a loading screen
 * with a custom message while async operations are in progress.
 */
public class LoadingManager {
    private final View overlayView;
    private final TextView loadingText;
    private final RadarView radarView;

    /**
     * Constructs a LoadingManager by finding the necessary views in the provided root view.
     * @param rootView The root view of the fragment or activity containing the loading overlay.
     */
    public LoadingManager(View rootView) {
        // Find views included in the fragment layout
        this.overlayView = rootView.findViewById(R.id.loading_overlay_container);
        this.loadingText = rootView.findViewById(R.id.loading_text);
        this.radarView = rootView.findViewById(R.id.radar_view);
    }

    /**
     * Shows the loading overlay with a custom message and starts the radar animation.
     * @param message The message to display (will be converted to uppercase).
     */
    public void show(String message) {
        if (overlayView == null) return;

        // Update text
        if (loadingText != null) {
            loadingText.setText(message.toUpperCase());
        }

        // Show layout
        overlayView.setVisibility(View.VISIBLE);
        overlayView.setAlpha(1f);

        // Start Radar Animation
        if (radarView != null) {
            radarView.startAnimation();
        }
    }

    /**
     * Hides the loading overlay and stops the radar animation.
     */
    public void hide() {
        if (overlayView == null) return;

        // Stop Radar Animation
        if (radarView != null) {
            radarView.stopAnimation();
        }

        // Hide layout
        overlayView.setVisibility(View.GONE);
    }
}

