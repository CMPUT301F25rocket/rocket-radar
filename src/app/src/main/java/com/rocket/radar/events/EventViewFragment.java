package com.rocket.radar.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

import java.text.DateFormat;
import java.util.ArrayList;

public class EventViewFragment extends Fragment {

    private static final String ARG_EVENT = "event";
    // 1. ADD ARG_IS_ORGANIZER CONSTANT
    private static final String ARG_IS_ORGANIZER = "is_organizer";
    private Event event;
    private ProfileViewModel profileViewModel;

    // 2. ADD isOrganizer aS A MEMBER VARIABLE
    private boolean isOrganizer;

    public EventViewFragment() {
        // Required empty public constructor
    }

    // This newInstance is for regular users
    public static EventViewFragment newInstance(Event event) {
        // Call the other newInstance, passing 'false' for the organizer flag
        return newInstance(event, false);
    }

    // This newInstance is for both organizers and regular users
    public static EventViewFragment newInstance(Event event, boolean isOrganizer) {
        EventViewFragment fragment = new EventViewFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        args.putBoolean(ARG_IS_ORGANIZER, isOrganizer); // Add the flag to the bundle
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
            // 3. RETRIEVE the isOrganizer flag from the bundle
            isOrganizer = getArguments().getBoolean(ARG_IS_ORGANIZER, false); // Default to false
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.event_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Find views
        Button backButton = view.findViewById(R.id.back_button);
        Button joinAndLeaveWaitlistButton = view.findViewById(R.id.join_and_leave_waitlist_button);
        // 4. DEFINE manageEntrantsButton
        Button manageEntrantsButton = view.findViewById(R.id.manage_entrants);
        TextView eventTitle = view.findViewById(R.id.event_title);
        TextView eventDate = view.findViewById(R.id.event_date);
        TextView eventDescription = view.findViewById(R.id.event_desc);
        TextView eventWaitlistSize = view.findViewById(R.id.waitlist_size);

        // Populate static event data
        if (event != null) {
            eventTitle.setText(event.getEventTitle());
            if (event.getDate() != null) { // Check event.getDate() for null
                String FormattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(event.getDate());
                eventDate.setText(FormattedDate);
            }
            eventDescription.setText(event.getDescription());
            int waitlistSize = event.getEventWaitlistIds().size();
            Log.d("EventViewFragment", "Waitlist size: " + waitlistSize);
            eventWaitlistSize.setText("People on waitlist: " + String.valueOf(waitlistSize));
        } else {
            Toast.makeText(getContext(), "Error: Event data missing.", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        // 5. THE LOGIC BLOCK CAN NOW USE THE DEFINED VARIABLES
        if (isOrganizer) {
            // Organizer View

            // 1. Configure the "Manage Entrants" button
            manageEntrantsButton.setVisibility(View.VISIBLE);
            manageEntrantsButton.setOnClickListener(v -> {
                OrganizerEntrantsFragment organizerEntrantsFragment = OrganizerEntrantsFragment.newInstance(event);
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, organizerEntrantsFragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            // 2. Repurpose the other button as "Edit"
            joinAndLeaveWaitlistButton.setVisibility(View.VISIBLE); // Make sure it is VISIBLE
            joinAndLeaveWaitlistButton.setText("Edit");
            // joinAndLeaveWaitlistButton.setOnClickListener(v -> handleEditEvent()); // Add your edit logic here

        } else {
            // Regular User View

            // Hide the organizer button
            manageEntrantsButton.setVisibility(View.GONE);

            // Configure the "Join/Leave Waitlist" button
            joinAndLeaveWaitlistButton.setVisibility(View.VISIBLE);
            joinAndLeaveWaitlistButton.setOnClickListener(v -> handleJoinLeaveWaitlist());
            profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
                updateWaitlistButton(joinAndLeaveWaitlistButton, profile);
            });
        }

        // Setup listeners
        backButton.setOnClickListener(v -> navigateBack());
        // REMOVED redundant listeners from here as they are now correctly placed inside the if/else block
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }

    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void handleJoinLeaveWaitlist() {
        ProfileModel currentProfile = profileViewModel.getProfileLiveData().getValue();
        if (currentProfile == null || event == null) {
            Toast.makeText(getContext(), "Error: Profile or event data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean onWaitlist = isOnWaitlist(currentProfile);

        if (onWaitlist) {
            currentProfile.removeOnWaitlistEventId(event.getEventId());
            currentProfile.removeOnMyEventId(event.getEventId());
            navigateBack();
            Toast.makeText(getContext(), "Removed from waitlist!", Toast.LENGTH_SHORT).show();
        } else {
            currentProfile.addOnWaitlistEventId(event.getEventId());
            currentProfile.addOnMyEventId(event.getEventId());
            navigateBack();
            Toast.makeText(getContext(), "Added to waitlist!", Toast.LENGTH_SHORT).show();
        }
        // After changing the profile, we must save it back to the ViewModel to persist the change
        profileViewModel.updateProfile(currentProfile);




        ArrayList<String> currentWaitlist = event.getEventWaitlistIds();
        currentWaitlist.add(currentProfile.getUid());
        event.setEventWaitlistIds(currentWaitlist);


    }

    private void updateWaitlistButton(Button button, ProfileModel profile) {
        if (event == null || profile == null) {
            button.setEnabled(false);
            return;
        }
        boolean onWaitlist = isOnWaitlist(profile);
        button.setText(onWaitlist ? "Leave Waitlist" : "Join Waitlist");
        button.setEnabled(true);
    }

    private boolean isOnWaitlist(ProfileModel profile) {
        if (profile == null || event == null || profile.getOnWaitlistEventIds() == null) {
            return false;
        }
        return profile.getOnWaitlistEventIds().contains(event.getEventId());
    }
}
