package com.rocket.radar.profile;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.SharedElementCallback;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This fragment is where the user can see their name, their events and their history.
 * It observes the ProfileViewModel and EventRepository for updates.
 */
public class ProfileFragment extends Fragment implements EventAdapter.OnEventListener {


    private ImageButton accountSettingsButton;
    private MaterialButton criteriaButton;
    private TextView profileName;

    private ProfileViewModel profileViewModel;
    private ProfileModel currentUserProfile;
    private RecyclerView myEventRecyclerView;
    private EventAdapter adapter;
    private MaterialButtonToggleGroup toggleGroup;
    private List<Event> allEvents;
    private List<Event> displayedEvents;
    private EventRepository eventRepository;
    private int lastClickedPosition = -1;


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
        View view =  inflater.inflate(R.layout.fragment_profile, container, false);
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

        postponeEnterTransition();

        // 3. Setup the Callback to map views when returning from Detail Screen
        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                // Make sure we have a valid position
                if (lastClickedPosition < 0) return;

                // Find the ViewHolder
                RecyclerView.ViewHolder selectedViewHolder =
                        myEventRecyclerView.findViewHolderForAdapterPosition(lastClickedPosition);

                if (selectedViewHolder == null || selectedViewHolder.itemView == null) return;

                // Map unique names to views
                ImageView image = selectedViewHolder.itemView.findViewById(R.id.event_background_image);
                TextView title = selectedViewHolder.itemView.findViewById(R.id.event_title_text);
                TextView date = selectedViewHolder.itemView.findViewById(R.id.date_text);

                if (image != null) sharedElements.put(names.get(0), image);
                if (title != null) sharedElements.put(names.get(1), title);
                if (date != null) sharedElements.put(names.get(2), date);
            }
        });

        eventRepository = EventRepository.getInstance();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        allEvents = new ArrayList<>();
        displayedEvents = new ArrayList<>();
        adapter = new EventAdapter(getContext(), displayedEvents, this);
        myEventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myEventRecyclerView.setAdapter(adapter);

        setupToggleListener();
        observeUserProfile();
        observeEvents();

        myEventRecyclerView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (lastClickedPosition == -1) {
                    myEventRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                    startPostponedEnterTransition();
                    return true;
                }

                RecyclerView.ViewHolder holder = myEventRecyclerView.findViewHolderForAdapterPosition(lastClickedPosition);
                if (holder == null) {
                    myEventRecyclerView.scrollToPosition(lastClickedPosition);
                    return false; // Wait for scroll
                }

                myEventRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
                startPostponedEnterTransition();
                return true;
            }
        });
    }

    /**
     * Sets up the toggle group listener to filter events when toggled.
     */
    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            filterAndDisplayEvents();
        });
    }

    /**
     * Observes changes in the user's profile and updates displayed events.
     */
    private void observeUserProfile() {
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            currentUserProfile = profile;
            filterAndDisplayEvents();
        });
    }

    /**
     * Observes all events from the repository and updates the displayed list.
     */
    private void observeEvents() {
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            Log.d("EventListFragment", "Data updated. " + newEvents.size() + " events received.");
            allEvents.clear();
            allEvents.addAll(newEvents);
            filterAndDisplayEvents();
        });
    }

    /**
     * Filters events based on the toggle selection and updates the adapter.
     */
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

    /**
     * Called when an event is clicked in the RecyclerView.
     * Opens the EventViewFragment for the selected event.
     * @param position The position of the clicked item in the adapter.
     */
    @Override
    public void onEventClick(int position, View itemView, ImageView imageView, TextView titleView, TextView dateView) {lastClickedPosition = position;
        Event selectedEvent = displayedEvents.get(position);

        // 1. Create the Bundle (Just like newInstance does internally)
        Bundle bundle = new Bundle();
        bundle.putSerializable("event", selectedEvent);

        // 2. Determine if the user is the organizer based on your filter logic
        // (If they are in the "My Events" tab, they are the organizer)
        boolean isOrganizer = (toggleGroup.getCheckedButtonId() == R.id.my_events_filter_button);

        // 3. Add the flag to the bundle manually
        // This string key "is_organizer" MUST match the constant in EventViewFragment
        bundle.putBoolean("is_organizer", isOrganizer);

        // 4. Setup transitions
        FragmentNavigator.Extras extras = new FragmentNavigator.Extras.Builder()
                .addSharedElement(imageView, ViewCompat.getTransitionName(imageView))
                .addSharedElement(titleView, ViewCompat.getTransitionName(titleView))
                .addSharedElement(dateView, ViewCompat.getTransitionName(dateView))
                .build();

        // 5. Navigate
        Navigation.findNavController(itemView).navigate(
                R.id.eventViewFragment,
                bundle,
                null,
                extras
        );
    }


    /**
     * Called when the fragment resumes. Re-observes events and restores UI visibility.
     */
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
