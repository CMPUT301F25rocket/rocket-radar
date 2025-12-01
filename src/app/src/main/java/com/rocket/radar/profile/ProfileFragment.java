package com.rocket.radar.profile;

import android.icu.text.ListFormatter;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventAdapter;
import com.rocket.radar.events.EventHistoryAdapter;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.events.EventViewFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This fragment is where the user can see their name, their events and their history.
 * It observes the ProfileViewModel and EventRepository for updates.
 */
public class ProfileFragment extends Fragment implements EventAdapter.OnEventListener, EventHistoryAdapter.OnEventListener {

    private ImageButton accountSettingsButton;
    private MaterialButton criteriaButton;
    private TextView profileName;

    private ProfileViewModel profileViewModel;
    private ProfileModel currentUserProfile;
    private RecyclerView myEventRecyclerView;
    private EventAdapter adapter;
    private EventHistoryAdapter historyAdapter;
    private MaterialButtonToggleGroup toggleGroup;
    private List<Event> allEvents;
    private List<Event> displayedEvents;
    private EventRepository eventRepository;


    /**
     * This fragment inflates the profile fragment layout, initializes UI elements,
     * and sets up the account settings button and profile name observer.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return the root view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        profileName = view.findViewById(R.id.profile_name);
        accountSettingsButton = view.findViewById(R.id.account_settings_button);
        accountSettingsButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profile_to_accountSettings);
        });

        criteriaButton = view.findViewById(R.id.app_criteria_button);
        criteriaButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profile_to_criteria);
        });

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null && profile.getName() != null) {
                profileName.setText(profile.getName());
            }
        });

        myEventRecyclerView = view.findViewById(R.id.my_event_recycler_view);
        toggleGroup = view.findViewById(R.id.profileToggleGroup);

        return view;
    }

    /**
     * Called after the view is created. Sets up the RecyclerView,
     * toggle listener, and observes profile and event data.
     * @param view The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        allEvents = new ArrayList<>();
        displayedEvents = new ArrayList<>();

        adapter = new EventAdapter(getContext(), displayedEvents, this);
        historyAdapter = new EventHistoryAdapter(getContext(), displayedEvents, currentUserProfile, this);

        myEventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myEventRecyclerView.setAdapter(adapter);

        setupToggleListener();
        observeUserProfile();
        observeEvents();
    }

    /**
     * Sets up the toggle group listener to filter events when toggled.
     */
    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                filterAndDisplayEvents();
            }
        });
    }

    /**
     * Observes changes in the user's profile and updates displayed events.
     */
    private void observeUserProfile() {
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            currentUserProfile = profile;
            historyAdapter = new EventHistoryAdapter(getContext(), displayedEvents, currentUserProfile, this);
            filterAndDisplayEvents();
        });
    }

    /**
     * Observes all events from the repository and updates the displayed list.
     */
    private void observeEvents() {
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            allEvents.clear();
            allEvents.addAll(newEvents);
            filterAndDisplayEvents();
        });
    }

    /**
     * Filters events based on the toggle selection and updates the adapter.
     */
    private void filterAndDisplayEvents() {
        if (allEvents == null || currentUserProfile == null) {
            return;
        }

        int checkedId = toggleGroup.getCheckedButtonId();
        List<Event> filteredList;

        // Get all lists from profile to avoid null pointer exceptions
        ArrayList<String> userWaitlistIds = currentUserProfile.getOnWaitlistEventIds() != null ? currentUserProfile.getOnWaitlistEventIds() : new ArrayList<>();
        ArrayList<String> userInvitedIds = currentUserProfile.getOnInvitedEventIds() != null ? currentUserProfile.getOnInvitedEventIds() : new ArrayList<>();

        ArrayList<String> userMyEventIds = currentUserProfile.getOnMyEventIds();
        if (userMyEventIds == null) {
            userMyEventIds = new ArrayList<>();
        }

        if (checkedId == R.id.my_events_filter_button) {
            ArrayList<String> finalUserMyEventIds = userMyEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> finalUserMyEventIds.contains(event.getEventId()))
                    .collect(Collectors.toList());
            myEventRecyclerView.setAdapter(adapter);
        } else if (checkedId == R.id.my_history_filter_button) {
            // Get current time for comparison
            long currentTime = System.currentTimeMillis();

            filteredList = allEvents.stream()
                    .filter(event -> {
                        // 1. Check if the user was involved (Invited, Waitlisted, or Attending)
                        boolean isInvited = userInvitedIds.contains(event.getEventId());
                        boolean isWaitlisted = userWaitlistIds.contains(event.getEventId());

                        // 2. Check if the event has passed
                        // Assuming event.getTimestamp() returns a Firestore Timestamp or similar
                        boolean hasPassed = false;
                        if (event.getEventStartDate() != null) {
                            hasPassed = event.getEventStartDate().getTime() < currentTime;
                        }

                        return (isInvited || isWaitlisted) && hasPassed;
                    })
                    .collect(Collectors.toList());

            // Switch to history adapter for "My History" to show status lines
            myEventRecyclerView.setAdapter(historyAdapter);

        } else {
            filteredList = new ArrayList<>();
            myEventRecyclerView.setAdapter(adapter);
        }

        displayedEvents.clear();
        displayedEvents.addAll(filteredList);
        if (myEventRecyclerView.getAdapter() != null) {
            myEventRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    /**
     * Called when an event is clicked in the RecyclerView.
     * Opens the EventViewFragment for the selected event.
     * @param position The position of the clicked item in the adapter.
     */
    @Override
    public void onEventClick(int position, View itemView) {
        Event selectedEvent = displayedEvents.get(position);

        Bundle bundle = new Bundle();
        bundle.putSerializable("event", selectedEvent);

        boolean isOrganizer = (toggleGroup.getCheckedButtonId() == R.id.my_events_filter_button);
        bundle.putBoolean("is_organizer", isOrganizer);

        Navigation.findNavController(itemView).navigate(
                R.id.eventViewFragment,
                bundle
        );
    }

    /**
     * Called when the fragment resumes. Re-observes events and restores UI visibility.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (eventRepository != null) {
            observeEvents();
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
