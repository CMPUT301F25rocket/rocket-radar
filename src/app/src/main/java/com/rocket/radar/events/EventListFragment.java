package com.rocket.radar.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView; // Import TextView
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
import com.rocket.radar.notifications.NotificationRepository; // Import NotificationRepository
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
    private TextView notificationBadge;
    private NotificationRepository notificationRepository;


    public EventListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_list, container, false);
        eventRecyclerView = view.findViewById(R.id.event_list_recycler_view);
        notificationButton = view.findViewById(R.id.btnNotification);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        notificationBadge = view.findViewById(R.id.notification_badge);


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

        notificationRepository = new NotificationRepository();


        notificationButton.setOnClickListener(v -> {
            NotificationFragment notificationFragment = new NotificationFragment();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, notificationFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        setupToggleListener();

        // Start observing data
        observeUserProfile();
        observeEvents();
        observeUnreadNotifications();
    }

    private void observeEvents() {
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            Log.d("EventListFragment", "Data updated. " + newEvents.size() + " events received.");
            allEvents.clear();
            allEvents.addAll(newEvents);
            filterAndDisplayEvents();
        });
    }

    private void observeUserProfile() {
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            currentUserProfile = profile;
            filterAndDisplayEvents();
        });
    }


    private void observeUnreadNotifications() {
        // Observe the list of notifications from the repository
        notificationRepository.getMyNotifications().observe(getViewLifecycleOwner(), notifications -> {
            // Filter the list to get a count of only the unread notifications
            long unreadCount = notifications.stream().filter(n -> !n.isReadStatus()).count();

            if (unreadCount > 0) {
                // If there are unread items, make the badge visible
                notificationBadge.setVisibility(View.VISIBLE);
                // Set the text to the number of unread items
                notificationBadge.setText(String.valueOf(unreadCount));
            } else {
                // If there are no unread items, hide the badge
                notificationBadge.setVisibility(View.GONE);
            }
        });
    }

    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            filterAndDisplayEvents();
        });
    }

    private void filterAndDisplayEvents() {
        if (allEvents == null || currentUserProfile == null) {
            return;
        }

        int checkedId = toggleGroup.getCheckedButtonId();
        List<Event> filteredList;

        ArrayList<String> userWaitlistEventIds = currentUserProfile.getOnWaitlistEventIds();
        if (userWaitlistEventIds == null) {
            userWaitlistEventIds = new ArrayList<>();
        }

        if (checkedId == R.id.discover_filter_button) {
            ArrayList<String> finalUserWaitlistEventIds = userWaitlistEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> !finalUserWaitlistEventIds.contains(event.getEventId()))
                    .collect(Collectors.toList());
        } else if (checkedId == R.id.waitlist_filter_button) {
            ArrayList<String> finalUserWaitlistEventIds1 = userWaitlistEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> finalUserWaitlistEventIds1.contains(event.getEventId()))
                    .collect(Collectors.toList());
        } else {
            filteredList = new ArrayList<>();
            //TODO: add other filter for attending
        }

        Log.d("EventListFragment", "Filtered list size: " + filteredList.size());
        displayedEvents.clear();
        displayedEvents.addAll(filteredList);
        adapter.notifyDataSetChanged();
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
