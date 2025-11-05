package com.rocket.radar.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventAdapter;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.events.EventViewFragment;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProfileFragment extends Fragment implements EventAdapter.OnEventListener {


    private ImageButton accountSettingsButton;
    private TextView profileName;

    private ProfileViewModel profileViewModel;
    private ProfileModel currentUserProfile;
    private RecyclerView myEventRecyclerView;
    private EventAdapter adapter;
    private MaterialButtonToggleGroup toggleGroup;
    private List<Event> allEvents;
    private List<Event> displayedEvents;
    private EventRepository eventRepository;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);
        profileName = view.findViewById(R.id.profile_name);
        accountSettingsButton = view.findViewById(R.id.account_settings_button);
        accountSettingsButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profile_to_accountSettings);
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventRepository = new EventRepository();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        allEvents = new ArrayList<>();
        displayedEvents = new ArrayList<>();
        adapter = new EventAdapter(getContext(), displayedEvents, this);
        myEventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myEventRecyclerView.setAdapter(adapter);

        setupToggleListener();
        observeUserProfile();
        observeEvents();
    }

    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            filterAndDisplayEvents();
        });
    }

    private void observeUserProfile() {
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            currentUserProfile = profile;
            filterAndDisplayEvents();
        });
    }

    private void observeEvents() {
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            Log.d("EventListFragment", "Data updated. " + newEvents.size() + " events received.");
            allEvents.clear();
            allEvents.addAll(newEvents);
            filterAndDisplayEvents();
        });
    }
    private void filterAndDisplayEvents() {
        // 1. Wait until both the profile and event list are loaded.
        if (allEvents == null || currentUserProfile == null) {
            return;
        }int checkedId = toggleGroup.getCheckedButtonId();
        List<Event> filteredList;

        // 2. Get the actual event IDs from the user's profile.
        ArrayList<String> userMyEventIds = currentUserProfile.getOnMyEventIds();
        if (userMyEventIds == null) {
            // Ensure the list is not null to prevent crashes.
            userMyEventIds = new ArrayList<>();
        }

        Log.d("ProfileFragment", "Filtering with " + userMyEventIds.size() + " event IDs for My Events.");


        if (checkedId == R.id.my_events_filter_button) {
            ArrayList<String> finalUserMyEventIds = userMyEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> finalUserMyEventIds.contains(event.getEventId()))
                    .collect(Collectors.toList());
        } else if (checkedId == R.id.my_history_filter_button) {
            ArrayList<String> finalUserMyEventIds1 = userMyEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> !finalUserMyEventIds1.contains(event.getEventId()))
                    .collect(Collectors.toList());
        } else {
            filteredList = new ArrayList<>(allEvents);
        }

        Log.d("EventListFragment", "Filtered list size: " + filteredList.size());
        displayedEvents.clear();
        displayedEvents.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onEventClick(int position) {
        Event clickedEvent = displayedEvents.get(position);
        EventViewFragment eventViewFragment = EventViewFragment.newInstance(clickedEvent);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, eventViewFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("EventListFragment", "onResume called.");
        if (eventRepository != null) {
            observeEvents();
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
