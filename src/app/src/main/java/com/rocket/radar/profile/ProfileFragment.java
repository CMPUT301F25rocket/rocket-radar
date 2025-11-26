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
    private int lastClickedPosition = -1;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        postponeEnterTransition();

        setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
                if (lastClickedPosition < 0) return;

                RecyclerView.ViewHolder selectedViewHolder =
                        myEventRecyclerView.findViewHolderForAdapterPosition(lastClickedPosition);

                if (selectedViewHolder == null || selectedViewHolder.itemView == null) return;

                ImageView image = selectedViewHolder.itemView.findViewById(R.id.event_background_image);
                TextView title = selectedViewHolder.itemView.findViewById(R.id.event_title_text);
                TextView date = selectedViewHolder.itemView.findViewById(R.id.date_text);

                if (image != null) sharedElements.put(names.get(0), image);
                if (title != null) sharedElements.put(names.get(1), title);
                if (date != null) sharedElements.put(names.get(2), date);
            }
        });

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

    private void setupToggleListener() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                filterAndDisplayEvents();
            }
        });
    }

    private void observeUserProfile() {
        profileViewModel.getProfileLiveData().observe(getViewLifecycleOwner(), profile -> {
            currentUserProfile = profile;
            historyAdapter = new EventHistoryAdapter(getContext(), displayedEvents, currentUserProfile, this);
            filterAndDisplayEvents();
        });
    }

    private void observeEvents() {
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            allEvents.clear();
            allEvents.addAll(newEvents);
            filterAndDisplayEvents();
        });
    }

    private void filterAndDisplayEvents() {
        if (allEvents == null || currentUserProfile == null) {
            return;
        }

        int checkedId = toggleGroup.getCheckedButtonId();
        List<Event> filteredList;

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
            // TODO: This logic is likely incorrect and needs to be updated based on what "history" means
            ArrayList<String> finalUserMyEventIds1 = userMyEventIds;
            filteredList = allEvents.stream()
                    .filter(event -> !finalUserMyEventIds1.contains(event.getEventId()))
                    .collect(Collectors.toList());
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

    @Override
    public void onEventClick(int position, View itemView, ImageView imageView, TextView titleView, TextView dateView) {
        lastClickedPosition = position;
        Event selectedEvent = displayedEvents.get(position);

        Bundle bundle = new Bundle();
        bundle.putSerializable("event", selectedEvent);

        boolean isOrganizer = (toggleGroup.getCheckedButtonId() == R.id.my_events_filter_button);
        bundle.putBoolean("is_organizer", isOrganizer);

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
        if (eventRepository != null) {
            observeEvents();
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
