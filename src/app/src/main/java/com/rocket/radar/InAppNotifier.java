package com.rocket.radar;

import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.snackbar.Snackbar;

/**
 * A utility class to show custom, non-intrusive in-app notification banners.
 * It cleverly uses a {@link Snackbar} as a base, repositioning it to the top
 * of the screen and replacing its layout with a custom one.
 * <p>
 * Author: Braden Woods
 */
public class InAppNotifier {

    /**
     * Shows a custom notification banner at the top of the screen.
     *
     * @param anchorView    The root view to anchor the banner to (e.g., CoordinatorLayout or the main layout from your activity).
     * @param title         The title of the notification.
     * @param description   The description text of the notification.
     * @param clickListener An optional {@link View.OnClickListener} to be executed when the banner is tapped.
     */
    public static void showNotification(@NonNull View anchorView,
                                        @NonNull String title,
                                        @NonNull String description,
                                        @Nullable View.OnClickListener clickListener) {

        // 1. Create a Snackbar but with an empty message. We only use it as a container.
        final Snackbar snackbar = Snackbar.make(anchorView, "", Snackbar.LENGTH_LONG);

        // 2. Access the Snackbar's root view. This is typically a FrameLayout.
        View snackbarLayout = snackbar.getView();

        // 3. Reposition the Snackbar to the top of the screen.
        //    By default, it appears at the bottom.
        ViewGroup.LayoutParams lp = snackbarLayout.getLayoutParams();
        if (lp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
            params.gravity = Gravity.TOP; // This is the key to moving it to the top.
            snackbarLayout.setLayoutParams(params);
        }

        // 4. Make the original Snackbar background transparent and remove its padding.
        //    This is crucial so that our custom layout can be displayed without interference.
        snackbarLayout.setPadding(0, 0, 0, 0);
        snackbarLayout.setBackgroundColor(Color.TRANSPARENT);

        // 5. Inflate our custom layout (in_app_notification_banner.xml).
        LayoutInflater inflater = LayoutInflater.from(anchorView.getContext());
        // TODO: Create the 'in_app_notification_banner.xml' layout file if it doesn't exist.
        View customView = inflater.inflate(R.layout.in_app_notification_banner, null);

        // 6. Add our custom view to the (now transparent) Snackbar layout.
        //    We ensure the layout is a ViewGroup before trying to add a child to it.
        if (snackbarLayout instanceof ViewGroup) {
            ((ViewGroup) snackbarLayout).addView(customView, 0);
        }

        // 7. Find the TextViews in our *custom* layout and populate them.
        TextView titleView = customView.findViewById(R.id.notification_title);
        TextView descriptionView = customView.findViewById(R.id.notification_description);
        titleView.setText(title);
        descriptionView.setText(description);

        // 8. Set the click listener on our custom view, not the Snackbar itself.
        if (clickListener != null) {
            customView.setOnClickListener(v -> {
                // When clicked, first dismiss the snackbar to hide it.
                snackbar.dismiss();
                // Then, execute the action provided by the calling code (e.g., navigate to the notification page).
                clickListener.onClick(v);
            });
        }

        // 9. Finally, show the modified Snackbar.
        snackbar.show();
    }
}
