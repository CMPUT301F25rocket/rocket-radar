package com.rocket.radar.loadingscreen;

import android.view.View;
import android.widget.TextView;
import com.rocket.radar.R;
import com.rocket.radar.loadingscreen.RadarView;

/**
 * A helper class that manages the visibility and state of the global loading overlay.
 *
 * <p>This manager encapsulates the logic for finding the loading views within the root layout,
 * updating the loading message, and controlling the custom {@link RadarView} animation.
 * It is designed to be instantiated by the main Activity and used to block user interaction
 * during long-running asynchronous operations.</p>
 *
 * <p><strong>Outstanding Issues:</strong>
 * <ul>
 *   <li>The view IDs (loading_overlay_container, etc.) are hardcoded, assuming they exist in the
 *       root view's hierarchy. This coupling requires the root layout to always include the specific loading stub.</li>
 * </ul>
 * </p>
 */
public class LoadingManager {
    private final View overlayView;
    private final TextView loadingText;
    private final RadarView radarView;

    /**
     * Constructs a new LoadingManager by finding the required views inside the provided root view.
     *
     * @param rootView The root view of the Activity or Fragment layout that contains the loading overlay includes.
     */
    public LoadingManager(View rootView) {
        // Find views included in the fragment layout
        this.overlayView = rootView.findViewById(R.id.loading_overlay_container);
        this.loadingText = rootView.findViewById(R.id.loading_text);
        this.radarView = rootView.findViewById(R.id.radar_view);
    }

    /**
     * Displays the loading overlay with a specific message and starts the radar animation.
     *
     * @param message The text to display (e.g., "Loading...", "Scanning...").
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
     * Hides the loading overlay and stops the radar animation to save resources.
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

