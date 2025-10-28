package com.rocket.radar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction; // Import FragmentTransaction

/**
 * A fragment that displays a list of events.
 * It uses the event_list.xml layout.
 */
public class EventListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.event_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find the notification button from the layout
        Button notificationButton = view.findViewById(R.id.btnNotification);

        // Set a click listener on the button
        notificationButton.setOnClickListener(v -> {
            // Create an instance of the destination fragment
            NotificationFragment notificationFragment = new NotificationFragment();

            // Perform the fragment transaction to navigate
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main, notificationFragment) // Assumes your FragmentContainerView in MainActivity is R.id.main
                        .addToBackStack(null) // Allows the user to press back to return to the event list
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .commit();
            }
        });

        // TODO: Set up other listeners for search, toggle buttons, and the RecyclerView
    }
}
