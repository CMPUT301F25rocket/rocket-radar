package com.rocket.radar.loadingscreen;

import android.view.View;
import android.widget.TextView;
import com.rocket.radar.R;
import com.rocket.radar.loadingscreen.RadarView;

public class LoadingManager {
    private final View overlayView;
    private final TextView loadingText;
    private final RadarView radarView;

    public LoadingManager(View rootView) {
        // Find views included in the fragment layout
        this.overlayView = rootView.findViewById(R.id.loading_overlay_container);
        this.loadingText = rootView.findViewById(R.id.loading_text);
        this.radarView = rootView.findViewById(R.id.radar_view);
    }

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

