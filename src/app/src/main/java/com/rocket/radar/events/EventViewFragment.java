package com.rocket.radar.events;

import android.os.Bundle;
import android.view.LayoutInflater;import android.view.View;
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

import java.util.stream.Collectors;

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

        Button backButton = view.findViewById(R.id.back_button);
        Button joinWaitlistButton = view.findViewById(R.id.join_waitlist_button);

        backButton.setOnClickListener(v -> {
            // This will pop the back stack and return to the EventListFragment
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        joinWaitlistButton.setOnClickListener(v -> {
            ProfileModel currentProfile = profileViewModel.getProfileLiveData().getValue();
            if (currentProfile != null && event != null) {
                currentProfile.addOnWaitlistEvent(event);
                profileViewModel.updateProfile(currentProfile);

                Toast.makeText(getContext(), "Added to waitlist!", Toast.LENGTH_SHORT).show();

                // Optional: Pop back to the previous screen
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });

        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null && event != null) {
                boolean onWaitlist = profile.getOnWaitlistEvents() != null && profile.getOnWaitlistEvents()
                        .stream().anyMatch(e -> e.getEventId().equals(event.getEventId()));

                if (onWaitlist) {
                    joinWaitlistButton.setText("On Waitlist");
                    joinWaitlistButton.setEnabled(false);
                } else {
                    joinWaitlistButton.setText("Join Waitlist");
                    joinWaitlistButton.setEnabled(true);
                }
            }
        });

        if (event != null) {
            TextView eventTitle = view.findViewById(R.id.event_title);
            TextView eventDate = view.findViewById(R.id.event_date);
            TextView eventTagline = view.findViewById(R.id.event_tagline);

            eventTitle.setText(event.getEventTitle());
            eventDate.setText(event.getDate());
            eventTagline.setText(event.getTagline());
        }
    }
}
