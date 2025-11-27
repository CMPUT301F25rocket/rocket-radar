package com.rocket.radar.events;

import android.app.SharedElementCallback;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView; // Import TextView
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.admin.AdminModeManager;
import com.rocket.radar.admin.AllNotificationsFragment;
import com.rocket.radar.databinding.CategoryChipBinding;
import com.rocket.radar.notifications.NotificationFragment;
import com.rocket.radar.notifications.NotificationRepository; // Import NotificationRepository
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This fragment is responsible for displaying a list of events.
 * It allows users to toggle between "Discover", "Waitlist", and "Attending" views.
 * It also provides access to notifications and event filtering options.
 * Outstanding issues: The "Attending" filter functionality is not yet implemented.
 */
public class EventListFragment extends Fragment implements EventAdapter.OnEventListener {
    private static final String ARG_EVENT = "event";
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
    private List<Integer> categories;
    private ChipGroup chipGroup;
    private ArrayList<String> selectedFilters;
    private FilterModel filterModel;
    private int lastClickedPosition = -1;

    public EventListFragment() {
        // Required empty public constructor
    }

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
        filterButton = view.findViewById(R.id.button_filter); // Initialize filter button
        chipGroup = view.findViewById(R.id.category_chip_group_event_list);
        selectedFilters = new ArrayList<>();
        filterModel = new ViewModelProvider(requireActivity()).get(FilterModel.class);

        Log.e("EventListFragment", "Lenght: " + filterModel.getFilters().getValue());

        for (var category : Event.allEventCategories) {
            CategoryChipBinding binding = CategoryChipBinding.inflate(inflater, chipGroup, false);
            binding.getRoot().setText(category);
            chipGroup.addView(binding.getRoot());
        }

        // initialize chips
        chipGroup.setVisibility(View.GONE);

        return view;
    }

@Override
public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    postponeEnterTransition();
    setExitSharedElementCallback(new SharedElementCallback() {
    @Override
    public void onMapSharedElements(java.util.List< java.lang.String> names, java.util.Map< java.lang.String, android.view.View> sharedElements) {
        // Locate the ViewHolder for the clicked position
        RecyclerView.ViewHolder selectedViewHolder = eventRecyclerView.findViewHolderForAdapterPosition(lastClickedPosition);

        if (selectedViewHolder == null) {
            return;
        }

        // Map the logic names to the actual views in the RecyclerView
        // We must check if 'names' (what the ViewFragment expects) matches what we have

        // Note: 'names' usually contains the transitionNames sent from the other fragment
        // We strictly map them to the views in our ViewHolder
        sharedElements.put(names.get(0), selectedViewHolder.itemView.findViewById(R.id.event_background_image));
        sharedElements.put(names.get(1), selectedViewHolder.itemView.findViewById(R.id.event_title_text));
        sharedElements.put(names.get(2), selectedViewHolder.itemView.findViewById(R.id.date_text));
    }
});


    // Initialization
        eventRepository = EventRepository.getInstance();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        allEvents = new ArrayList<>();
        displayedEvents = new ArrayList<>();
        adapter = new EventAdapter(getContext(), displayedEvents, this);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.setAdapter(adapter);

        eventRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                // If no position was clicked, just start normally
                if (lastClickedPosition == -1) {
                    eventRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    startPostponedEnterTransition();
                    return true;
                }

                RecyclerView.ViewHolder holder = eventRecyclerView.findViewHolderForAdapterPosition(lastClickedPosition);

                // If the holder is null, it means the item is off-screen.
                if (holder == null) {
                    eventRecyclerView.scrollToPosition(lastClickedPosition);
                    // RETURN FALSE: This cancels the current frame draw and waits for the scroll to finish.
                    // The listener will be called again on the next frame.
                    return false;
                }

                // The view is ready! Remove listener and start animation.
                eventRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                startPostponedEnterTransition();
                return true;
            }
        });


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

    private void setExitSharedElementCallback(SharedElementCallback sharedElementCallback) {
    }

    private void observeEvents() {
        // START LOADING
        if (getActivity() instanceof MainActivity) {        ((MainActivity) getActivity()).setLoading(true, "Scanning Events...");
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
        List<Event> filteredList;
        if (allEvents == null || currentUserProfile == null) {
            return;
        }
        // all selected by default so that all show, but visibility will still be set to gone for now
        // if an event has the chip name in its categories field, show that event
        // for testing purposes, we will hardcode the selectedFilters to have "Business" and "Food"

        int checkedId = toggleGroup.getCheckedButtonId();

        ArrayList<String> userWaitlistEventIds = currentUserProfile.getOnWaitlistEventIds();
        if (userWaitlistEventIds == null) {
            userWaitlistEventIds = new ArrayList<>();
        }

        ArrayList<String> userAttendingEventIds = currentUserProfile.getAttendingEventIds();
        if (userAttendingEventIds == null) {
            userAttendingEventIds = new ArrayList<>();
        }



        if (checkedId == R.id.discover_filter_button) {
            ArrayList<String> finalUserWaitlistEventIds = userWaitlistEventIds;

            // display events who have the selectedFIlters as items in their categories attribute
            for (Event event : allEvents) {
                Log.d("EventListFragment", "Event: " + event.toString());
                //Log.d("EventListFragment", "categories of event: " + event.getCategories().toString());
                Log.d("EventListFragment", "date of event: " + event.getEventStartDate().toString());
            }
            selectedFilters = filterModel.getFilters().getValue();
            if (selectedFilters.size() > 0) {
                // Set selected chips
                for (var selected : selectedFilters) {
                    for (int i = 0; i < chipGroup.getChildCount(); ++i) {
                        View view = chipGroup.getChildAt(i);
                        if (view instanceof Chip) {
                            Chip chip = (Chip) view;
                            if (chip.getText().toString().equals(selected)) {
                                chip.setChecked(true);
                            }
                        }
                    }
                }

                // also set the chips visibility
                chipGroup.setVisibility(View.VISIBLE);

                // finally, display the filtered eventrs
                ArrayList<String> finalSelectedFilters = selectedFilters;

                Date selectedDate = filterModel.getDate().getValue();
                if (selectedDate == null) {
                    Log.e("EventListFragment", "selected date is null");
                }



                List<Event> intermediateList = allEvents.stream()
                        .filter(event -> event.getCategories().containsAll(finalSelectedFilters))
                        .filter(event -> !finalUserWaitlistEventIds.contains(event.getEventId()))
                        .collect(Collectors.toList());

                if (selectedDate != null) {
                    Log.d("EventListFragment", "date selected: + " + selectedDate);

                    for (Event event : allEvents) {
                        Log.d("EventListFragment", "Event: " + event.toString());
                        //Log.d("EventListFragment", "categories of event: " + event.getCategories().toString());
                        Log.d("EventListFragment", "date of event: " + event.getEventStartDate().toString());

                        Log.d("EventListFragment", "is date of event after selected date?: " + event.getEventStartDate().after(selectedDate));
                    }

                    filteredList = intermediateList.stream()
                            .filter(event -> event.getEventStartDate().after(selectedDate))
                            .collect(Collectors.toList());
                } else {
                    filteredList = intermediateList;
                }
            } else {
                chipGroup.setVisibility(View.GONE);
                ArrayList<String> finalUserWaitlistEventIds3 = userWaitlistEventIds;
                filteredList = allEvents.stream()
                        .filter(event -> !finalUserWaitlistEventIds3.contains(event.getEventId()))
                        .collect(Collectors.toList());
            }
        } else if (checkedId == R.id.waitlist_filter_button) {
            ArrayList<String> finalUserWaitlistEventIds1 = userWaitlistEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> finalUserWaitlistEventIds1.contains(event.getEventId()))
                    .collect(Collectors.toList());
        } else {
            ArrayList<String> finalUserAttendingEventIds1 = userAttendingEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> finalUserAttendingEventIds1.contains(event.getEventId()))
                    .collect(Collectors.toList());
        }



        Log.d("EventListFragment", "Filtered list size: " + filteredList.size());
        displayedEvents.clear();
        displayedEvents.addAll(filteredList);
        adapter.notifyDataSetChanged();
    }

    // Update the method signature and add the shared element
    @Override
    public void onEventClick(int position, View itemView, ImageView imageView, TextView titleView, TextView dateView) {
        lastClickedPosition = position;
        Event selectedEvent = displayedEvents.get(position);

        Bundle bundle = new Bundle();
        bundle.putSerializable("event", selectedEvent);

        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(imageView, ViewCompat.getTransitionName(imageView))
                .addSharedElement(titleView, ViewCompat.getTransitionName(titleView))
                .addSharedElement(dateView, ViewCompat.getTransitionName(dateView))
                .build();

        Navigation.findNavController(itemView).navigate(
                R.id.eventViewFragment,
                bundle,
                null,
                extras
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
