package com.rocket.radar;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * A DialogFragment for showing an RSVP-style notification.
 * This dialog allows the user to accept or decline an invitation and should be shown
 * when a user clicks on a 'SELECTED_ENTRANTS' notification.
 * <p>
 * Author: Braden Woods
 */
public class RsvpDialogFragment extends DialogFragment {

    // TODO: Add arguments to pass data like eventId into this dialog.
    // private static final String ARG_EVENT_ID = "arg_event_id";

    // TODO: Create a newInstance() factory method similar to NotificationInfoDialogFragment
    // public static RsvpDialogFragment newInstance(String eventId) { ... }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the custom layout for the RSVP dialog.
        View view = inflater.inflate(R.layout.notification_rsvp, container, false);

        // --- Find and set up the buttons ---
        Button declineButton = view.findViewById(R.id.button_decline);
        Button acceptButton = view.findViewById(R.id.button_accept);

        declineButton.setOnClickListener(v -> {
            // TODO: Implement the database logic for declining the event invitation.
            // This would likely involve:
            // 1. Getting the current user's ID.
            // 2. Getting the event ID (passed via arguments).
            // 3. Updating the event's attendee list in Firestore to mark the user as 'declined'.
            // 4. Potentially giving their spot to someone on the waitlist.
            Toast.makeText(getContext(), "RSVP Declined (Logic not implemented)", Toast.LENGTH_SHORT).show();
            dismiss(); // Close the dialog
        });

        acceptButton.setOnClickListener(v -> {
            // TODO: Implement the database logic for accepting the event invitation.
            // This would involve:
            // 1. Getting the current user's ID.
            // 2. Getting the event ID.
            // 3. Updating the event's attendee list in Firestore to confirm their spot.
            Toast.makeText(getContext(), "RSVP Accepted (Logic not implemented)", Toast.LENGTH_SHORT).show();
            dismiss(); // Close the dialog
        });

        return view;
    }

    /**
     * Called to create the dialog. We use this to customize its appearance.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // This is crucial for a modern look. It removes the default title bar, allowing
        // our custom layout to control the entire appearance.
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /**
     * Called when the fragment's view is created. We use this to style the dialog's window.
     */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // 1. Make the dialog's window background transparent. This is essential to let the
            //    rounded corners of our CardView layout be visible.
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // 2. Set the dialog's dimensions. We make it take up 90% of the screen width
            //    and wrap its content's height for a standard, modern dialog feel.
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
