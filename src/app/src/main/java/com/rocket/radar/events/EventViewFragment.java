package com.rocket.radar.events;

import android.os.Bundle;
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

import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

import java.text.DateFormat;
import java.util.Date;

// 1. Extend Fragment
public class EventViewFragment extends Fragment {

    // 2. Add key for arguments
    private static final String ARG_EVENT = "event";
    private Event event;
    private ProfileViewModel profileViewModel;

    // 3. Add a required empty public constructor
    public EventViewFragment() {
    }

    // 4. Create a static newInstance method to pass the event object
    public static EventViewFragment newInstance(Event event) {
        EventViewFragment fragment = new EventViewFragment();
        Bundle args = new Bundle();
        // The Event class MUST implement Serializable for this to work
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    // 5. Use onCreate to retrieve the arguments
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
        }
    }

    // 6. Use onCreateView as before, but make it an override
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // DONE: View inflation only — all initialization moved to onViewCreated()
        return inflater.inflate(R.layout.event_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Find views
        Button backButton = view.findViewById(R.id.back_button);
        Button joinAndLeaveWaitlistButton = view.findViewById(R.id.join_and_leave_waitlist_button);
        TextView eventTitle = view.findViewById(R.id.event_title);
        TextView eventDate = view.findViewById(R.id.event_date);
        TextView eventTagline = view.findViewById(R.id.event_tagline);

        // Populate static event data
        if (event != null) {
            eventTitle.setText(event.getEventTitle());
            String FormattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(event.getDate());
            eventDate.setText(FormattedDate);
            eventTagline.setText(event.getTagline());
        } else {
            // If there's no event data, there's nothing to show.
            navigateBack();
            return;
        }

        // Setup listeners
        backButton.setOnClickListener(v -> navigateBack());
        joinAndLeaveWaitlistButton.setOnClickListener(v -> handleJoinLeaveWaitlist());

        // Observe LiveData to update UI dynamically
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            // The button state depends on the profile and event.
            updateWaitlistButton(joinAndLeaveWaitlistButton, profile);
        });
    }

    private void navigateBack() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }

    private void handleJoinLeaveWaitlist() {
        ProfileModel currentProfile = profileViewModel.getProfileLiveData().getValue();
        // Check for null profile or event to prevent crashes.
        if (currentProfile == null || event == null) {
            Toast.makeText(getContext(), "Error: Profile or event data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean onWaitlist = isOnWaitlist(currentProfile);

        if (onWaitlist) {
            currentProfile.removeOnWaitlistEventId(event.getEventId());
            Toast.makeText(getContext(), "Removed from waitlist!", Toast.LENGTH_SHORT).show();
        } else {
            currentProfile.addOnWaitlistEventId(event.getEventId());
            Toast.makeText(getContext(), "Added to waitlist!", Toast.LENGTH_SHORT).show();
        }

        profileViewModel.updateProfile(currentProfile);
        navigateBack(); // Navigate back after the action.
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
        // A series of checks to ensure we don't get a NullPointerException.
        if (profile == null || event == null || profile.getOnWaitlistEventIds() == null) {
            return false;
        }
        // Use the stream API to check for the presence of the event ID.
        return profile.getOnWaitlistEventIds().stream().anyMatch(id -> id.equals(event.getEventId()));
    }
}
