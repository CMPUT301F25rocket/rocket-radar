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

import com.google.android.material.button.MaterialButton;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

import java.text.DateFormat;

public class EventViewFragment extends Fragment {

    private static final String ARG_EVENT = "event";
    private Event event;
    private ProfileViewModel profileViewModel;

    public EventViewFragment() {
        // Required empty public constructor
    }

    public static EventViewFragment newInstance(Event event) {
        EventViewFragment fragment = new EventViewFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
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
        TextView eventTitle = view.findViewById(R.id.event_title);
        TextView eventDate = view.findViewById(R.id.event_date);
        TextView eventDescription = view.findViewById(R.id.event_desc);

        // Populate static event data
        if (event != null) {
            // Use correct getters
            eventTitle.setText(event.getEventTitle());
            if (event.getEventTitle() != null) {
                String FormattedDate = DateFormat.getDateInstance(DateFormat.FULL).format(event.getDate());
                eventDate.setText(FormattedDate);
            }
            eventDescription.setText(event.getDescription());
        } else {
            Toast.makeText(getContext(), "Error: Event data missing.", Toast.LENGTH_SHORT).show();
            navigateBack();
            return;
        }

        // Setup listeners
        backButton.setOnClickListener(v -> navigateBack());
        joinAndLeaveWaitlistButton.setOnClickListener(v -> handleJoinLeaveWaitlist());

        // Observe LiveData to update UI dynamically
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            updateWaitlistButton(joinAndLeaveWaitlistButton, profile);
        });
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
            Toast.makeText(getContext(), "Removed from waitlist!", Toast.LENGTH_SHORT).show();
        } else {
            currentProfile.addOnWaitlistEventId(event.getEventId());
            currentProfile.addOnMyEventId(event.getEventId());
            Toast.makeText(getContext(), "Added to waitlist!", Toast.LENGTH_SHORT).show();
        }
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
