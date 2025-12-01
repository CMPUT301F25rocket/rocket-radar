package com.rocket.radar.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView; 
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.admin.AdminModeManager;
import com.rocket.radar.databinding.CategoryChipBinding;
import com.rocket.radar.notifications.NotificationRepository; 
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This fragment is responsible for displaying a list of events.
 * It allows users to toggle between "Discover", "Waitlist", and "Attending" views.
 * It also provides access to notifications and event filtering options.
 * Outstanding issues: The "Attending" filter functionality is not yet implemented.
 */
public class EventListFragment extends Fragment implements EventAdapter.OnEventListener {
    private static final String ARG_EVENT = "event";
    private static final String TAG = EventListFragment.class.getSimpleName();
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
    private Button filterButton;
    private ChipGroup chipGroup;
    private FilterModel filterModel;

    public EventListFragment() {
        // Required empty public constructor
    }

    /**
     * Factory method to create a new instance of EventListFragment with an event parameter.
     * @param event the event to pass to the fragment
     * @return a new instance of EventListFragment
     */
    public static EventListFragment newInstance(Event event) {
        EventListFragment fragment = new EventListFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_list, container, false);
        eventRecyclerView = view.findViewById(R.id.event_list_recycler_view);
        notificationButton = view.findViewById(R.id.btnNotification);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        notificationBadge = view.findViewById(R.id.notification_badge);
        filterButton = view.findViewById(R.id.button_filter);
        chipGroup = view.findViewById(R.id.category_chip_group_event_list);
        filterModel = new ViewModelProvider(requireActivity()).get(FilterModel.class);

        // We want to hide the filter header by default.
        chipGroup.setVisibility(View.GONE);
        chipGroup.setOnCheckedStateChangeListener(this::removeActiveFilter);
        return view;
    }

    /**
     * Removes filters that are no longer checked in the chip group.
     * This method is called when the checked state of chips changes.
     * @param group the chip group containing filter chips
     * @param selected the list of selected chip IDs
     */
    public void removeActiveFilter(ChipGroup group, List<Integer> selected) {
        for (int i = 0; i < group.getChildCount(); ++i) {
            View child = group.getChildAt(i);
            if (!(child instanceof Chip)) continue;
            Chip chip = (Chip)child;
            if (chip.isChecked()) continue;
            String filterName = chip.getText().toString();
            filterModel.removeFilterByName(filterName);
        }
        filterAndDisplayEvents();
    }

    /**
     * Displays an active filter as a chip in the chip group.
     * @param filter the event filter to display as a chip
     */
    public void showActiveFilter(FilterModel.EventFilter filter) {
        CategoryChipBinding binding = CategoryChipBinding.inflate(getLayoutInflater(), chipGroup, false);
        binding.getRoot().setText(filter.getFilterName());
        binding.getRoot().setChecked(true);
        chipGroup.addView(binding.getRoot());
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialization
        eventRepository = EventRepository.getInstance();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        allEvents = new ArrayList<>();
        displayedEvents = new ArrayList<>();
        adapter = new EventAdapter(getContext(), displayedEvents, this);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.setAdapter(adapter);

        notificationRepository = new NotificationRepository();

        notificationButton.setOnClickListener(v -> {
            AdminModeManager adminModeManager = AdminModeManager.getInstance(getContext());
            if (currentUserProfile != null && currentUserProfile.getRole() == ProfileModel.UserRole.ADMIN && adminModeManager.isAdminModeOn()) {
                Navigation.findNavController(v).navigate(R.id.action_event_list_fragment_to_all_notifications);
            } else {
                Navigation.findNavController(v).navigate(R.id.notificationFragment);
            }
        });

        filterButton.setOnClickListener(v -> {
            FilterEventsFragment filterFragment = new FilterEventsFragment();
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, filterFragment)
                    .addToBackStack(null) // Allows user to go back to the event list
                    .commit();
            }
        });

        setupToggleListener();

        // Start observing data
        observeUserProfile();
        observeEvents();
        observeUnreadNotifications();
    }

    /**
     * Observes changes to the event repository and updates the displayed events list.
     * Shows a loading indicator while fetching events.
     */
    private void observeEvents() {
        // START LOADING
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setLoading(true, "Scanning Events...");
        }

        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            Log.d("EventListFragment", "Data updated. " + newEvents.size() + " events received.");
            allEvents.clear();
            allEvents.addAll(newEvents);
            filterAndDisplayEvents();

            // STOP LOADING
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setLoading(false);
            }
        });
    }

    /**
     * Observes changes to the user profile and updates the UI accordingly.
     * Updates the displayed events and notification button UI when profile changes.
     */
    private void observeUserProfile() {
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            currentUserProfile = profile;
            filterAndDisplayEvents();
            updateNotificationButtonUI(profile);
        });
    }

    /**
     * This function updates the bell to be greyed out and the badge to be hidden
     * when notifications are off, otherwise both ui components are visible.
     * @param profile the current user profile that is using the app.
     */
    private void updateNotificationButtonUI(ProfileModel profile) {
        if (profile == null) {
            notificationButton.setAlpha(0.5f);
            notificationBadge.setAlpha(0.0f);
            return;
        }

        Boolean isEnabled = profile.isNotificationsEnabled();

        if (Boolean.TRUE.equals(isEnabled)) {
            notificationButton.setAlpha(1.0f);
            notificationBadge.setAlpha(1.0f);
        } else {
            notificationButton.setAlpha(0.5f);
            notificationBadge.setAlpha(0.0f);
        }

        AdminModeManager adminModeManager = AdminModeManager.getInstance(getContext());
        if (currentUserProfile != null && currentUserProfile.getRole() == ProfileModel.UserRole.ADMIN && adminModeManager.isAdminModeOn()) {
            notificationBadge.setAlpha(0.0f);
        } else {
            notificationBadge.setAlpha(1.0f);
        }
    }

    /**
     * Observes unread notifications and updates the notification badge.
     * Displays the count of unread notifications or hides the badge if there are none.
     */
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

    /**
     * Sets up a listener for the toggle button group that filters events when selection changes.
     * This allows users to switch between Discover, Waitlist, and Attending views.
     */
    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            filterAndDisplayEvents();
        });
    }

    /**
     * Filters and displays events based on the selected feed (Discover, Waitlist, or Attending)
     * and any active filters from the filter model.
     * Updates the chip group to show active filters and refreshes the RecyclerView adapter.
     */
    private void filterAndDisplayEvents() {
        if (allEvents == null || currentUserProfile == null) {
            return;
        }

        int selectedEventFeed = toggleGroup.getCheckedButtonId();

        final ArrayList<String> userWaitlistEventIds = currentUserProfile.getOnWaitlistEventIds();
        final ArrayList<String> userAttendingEventIds = currentUserProfile.getAttendingEventIds();

        displayedEvents.clear();

        // See if there are any active filters imposed by the filter model and if there are display
        // them as chips.
        chipGroup.removeAllViews();
        if (filterModel.getFilters().findAny().isPresent()) {
            chipGroup.setVisibility(View.VISIBLE);
            // We will first add the date tag if it exist so that there is a little structure to the
            // display.
            filterModel.getFilters()
                    .filter(eventFilter -> eventFilter.getFilterCategory().equals("eventDate"))
                    .forEach(this::showActiveFilter);

            // Then we can go on to display the category filters for the event.
            filterModel.getFilters()
                    .filter(eventFilter -> eventFilter.getFilterCategory().equals("category"))
                    .forEach(this::showActiveFilter);
        } else {
            chipGroup.setVisibility(View.GONE);
        }

        // Fetch the events the current feed should contain by default.
        Stream<Event> eventsForFeed;
        if (selectedEventFeed == R.id.discover_filter_button) {
            eventsForFeed = allEvents.stream()
                    .filter(event -> userWaitlistEventIds == null
                            || !userWaitlistEventIds.contains(event.getEventId()))
                    .filter(event -> userAttendingEventIds == null
                            || !userAttendingEventIds.contains(event.getEventId()));
        } else if (selectedEventFeed == R.id.waitlist_filter_button) {
            eventsForFeed = allEvents.stream()
                    .filter(event -> userWaitlistEventIds == null
                            || userWaitlistEventIds.contains(event.getEventId()));
        } else if (selectedEventFeed == R.id.attending_filter_button) {
            eventsForFeed = allEvents.stream()
                    .filter(event ->  userAttendingEventIds == null
                            || userAttendingEventIds.contains(event.getEventId()));
        } else {
            Log.e(TAG, "Unexpected event feed selected with id" + selectedEventFeed);
            return;
        }

        filterModel
            .filter(eventsForFeed)
            .forEach(event -> displayedEvents.add(event));

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onEventClick(int position, View itemView) {
        Event selectedEvent = displayedEvents.get(position);

        Bundle bundle = new Bundle();
        bundle.putSerializable("event", selectedEvent);

        Navigation.findNavController(itemView).navigate(
                R.id.eventViewFragment,
                bundle
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("EventListFragment", "onResume called.");
        if (eventRepository != null) {
            observeEvents();
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(android.view.View.VISIBLE);
        }

        filterAndDisplayEvents();
    }
}
