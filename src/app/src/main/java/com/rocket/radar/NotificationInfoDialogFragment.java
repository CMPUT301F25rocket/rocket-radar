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
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * A general-purpose, reusable dialog for displaying information with a title and message.
 * It uses the factory method pattern for safe argument passing.
 * <p>
 * Author: Braden Woods
 */
public class NotificationInfoDialogFragment extends DialogFragment {

    // Use constants for argument keys. This prevents typos and makes code easier to maintain.
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_MESSAGE = "arg_message";

    /**
     * The public factory method for creating instances of this dialog.
     * This is the safest way to pass data to a fragment, as the system can restore
     * the arguments bundle for you during configuration changes.
     *
     * @param title   The title to be displayed in the dialog.
     * @param message The main body message of the dialog.
     * @return A new instance of NotificationInfoDialogFragment.
     */
    public static NotificationInfoDialogFragment newInstance(String title, String message) {
        NotificationInfoDialogFragment fragment = new NotificationInfoDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_info, container, false);
        TextView titleTextView = view.findViewById(R.id.dialog_title);
        TextView messageTextView = view.findViewById(R.id.dialog_message);
        Button okButton = view.findViewById(R.id.button_ok);

        // Retrieve arguments to populate the UI.
        if (getArguments() != null) {
            titleTextView.setText(getArguments().getString(ARG_TITLE));
            messageTextView.setText(getArguments().getString(ARG_MESSAGE));
        }

        // The "OK" button simply closes the dialog.
        okButton.setOnClickListener(v -> dismiss());

        return view;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
