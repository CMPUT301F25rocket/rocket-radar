package com.rocket.radar.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.notifications.NotificationFragment;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventListFragment extends Fragment implements EventAdapter.OnEventListener {

    private RecyclerView eventRecyclerView;
    private EventAdapter adapter;
    private List<Event> displayedEvents;
    private List<Event> allEvents;
    private EventRepository eventRepository;
    private ProfileViewModel profileViewModel;
    private ProfileModel currentUserProfile;
    private Button notificationButton;
    private MaterialButtonToggleGroup toggleGroup;


    public EventListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_list, container, false);
        eventRecyclerView = view.findViewById(R.id.event_list_recycler_view);
        notificationButton = view.findViewById(R.id.btnNotification);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialization
        eventRepository = new EventRepository();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        allEvents = new ArrayList<>();
        displayedEvents = new ArrayList<>();
        adapter = new EventAdapter(getContext(), displayedEvents, this);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.setAdapter(adapter);

        notificationButton.setOnClickListener(v -> {
            // Create the NotificationFragment using its empty constructor.
            // It does not need any arguments.
            NotificationFragment notificationFragment = new NotificationFragment();

            if (getActivity() instanceof MainActivity) { // Check if the activity is MainActivity
                // Hide the bottom navigation view
                ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);

                getActivity().getSupportFragmentManager().beginTransaction()
                        // Use your actual FragmentContainerView ID
                        .replace(R.id.nav_host_fragment, notificationFragment)
                        .addToBackStack(null) // Allows the user to return with the back button
                        .commit();
            }
        });

        setupToggleListener();

        // Start observing data
        observeUserProfile();
        observeEvents();

        // Optional: Call this once if you want to ensure dummy data exists in
        // Firestore.
        // You can comment this out after the first run.
        //eventRepository.addDummyDatatodb();
    }

    private void observeEvents() {
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            //if (newEvents != null) {
            Log.d("EventListFragment", "Data updated. " + newEvents.size() + " events received.");
            allEvents.clear();
            allEvents.addAll(newEvents);
            filterAndDisplayEvents();
            //}
        });
    }

    private void observeUserProfile() {
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            currentUserProfile = profile;
            filterAndDisplayEvents(); // Refilter events when profile updates
        });
    }

    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            filterAndDisplayEvents();
        });
    }

    private void filterAndDisplayEvents() {
        if (allEvents == null || currentUserProfile == null) {
            return; // Not ready to filter
        }

        int checkedId = toggleGroup.getCheckedButtonId();
        List<Event> filteredList = new ArrayList<>();

        ArrayList<String> userWaitlistEventIds = new ArrayList<>();
        if (!currentUserProfile.getOnWaitlistEventIds().isEmpty()) {
            Log.d("EventListFragment", "User has " + currentUserProfile.getOnWaitlistEventIds().size() + " events on waitlist.");
            userWaitlistEventIds.addAll(currentUserProfile.getOnWaitlistEventIds());
        }
        if (checkedId == R.id.discover_filter_button) {
            filteredList = allEvents.stream()
                    .filter(event -> userWaitlistEventIds.isEmpty() || !userWaitlistEventIds.contains(event.getEventId()))
                    .collect(Collectors.toList());
        } else if (checkedId == R.id.waitlist_filter_button) {
            if (!userWaitlistEventIds.isEmpty()) {
                filteredList = allEvents.stream()
                        .filter(event -> userWaitlistEventIds.contains(event.getEventId()))
                        .collect(Collectors.toList());
            }
        } else {
            filteredList = allEvents;
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
        super.onResume();// This is crucial. When we return to this fragment, the underlying user profile
        // data might have changed (e.g., an event was added to the waitlist).
        // By calling filterAndDisplayEvents() here, we force the UI to re-evaluate
        // the filters with the latest data from the ViewModel. We remove this because LiveData observers handle it.

        Log.d("EventListFragment", "onResume called.");
        // Re-fetch events to ensure the list is up-to-date,
        // for example if an event was added or modified.
        // The observer will then handle updating the UI.
        // This is a simple way to trigger a refresh. For a more sophisticated
        // app, you might only re-fetch if data is considered stale.
        // The LiveData observer on getAllEvents() will be triggered
        // by this, which in turn calls filterAndDisplayEvents().
        if (eventRepository != null) {
            observeEvents();
        }

        // Also, ensure the bottom nav bar is visible when returning to this screen.
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
