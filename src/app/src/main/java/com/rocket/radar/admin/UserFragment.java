package com.rocket.radar.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventAdapter;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.events.EventViewFragment;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserFragment extends Fragment  implements EventAdapter.OnEventListener {
    private static final String USER_PROFILE = "userProfile";
    private ProfileModel userProfile;
    private AdminRepository adminRepository;
    public static UserFragment newInstance(ProfileModel profile) {
        UserFragment fragment = new UserFragment();
        Bundle args = new Bundle();
        args.putSerializable(USER_PROFILE, profile); // ProfileModel implements Serializable
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userProfile = (ProfileModel) getArguments().getSerializable(USER_PROFILE);
        }
    }

    private ImageButton deleteButton;

    private MaterialButton backButton;
    private TextView profileName, profilePhone, profileEmail;
    private ProfileViewModel profileViewModel;
    private ProfileModel currentUserProfile;
    private RecyclerView myEventRecyclerView;
    private EventAdapter adapter;
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
        View view =  inflater.inflate(R.layout.fragment_user, container, false);
        profileName = view.findViewById(R.id.profile_name);
        deleteButton = view.findViewById(R.id.delete_button);
        profilePhone = view.findViewById(R.id.profile_phone);
        profileEmail = view.findViewById(R.id.profile_email);
        deleteButton.setOnClickListener(v -> {
            String currentUserUid = profileViewModel.getProfileLiveData().getValue().getUid();
            String viewedUserUid = userProfile.getUid();

            if (currentUserUid.equals(viewedUserUid)) { // prevent self-deletion
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Action not allowed")
                        .setMessage("You cannot delete your own account from the admin panel. Go to Account Settings instead.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete this user?")
                    .setMessage("This will permanently remove this profile and all associated data. This action cannot be undone.")
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setPositiveButton("Delete User", (dialog, which) -> {
                        confirmAccountDelete();
                    })
                    .show();
        });

        myEventRecyclerView = view.findViewById(R.id.my_event_recycler_view);
        toggleGroup = view.findViewById(R.id.profileToggleGroup);

        backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_user_to_browse_users);
        });
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

        eventRepository = EventRepository.getInstance();
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        allEvents = new ArrayList<>();
        displayedEvents = new ArrayList<>();
        adapter = new EventAdapter(getContext(), displayedEvents, this);
        myEventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        myEventRecyclerView.setAdapter(adapter);

        currentUserProfile = userProfile;
        profileName.setText(currentUserProfile.getName());
        profileEmail.setText(currentUserProfile.getEmail());
        profilePhone.setText(currentUserProfile.getPhoneNumber());
        adminRepository = new AdminRepository();
        filterAndDisplayEvents();

        setupToggleListener();
        observeEvents();
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
    public void onEventClick(int position) {
        Event clickedEvent = displayedEvents.get(position);

        boolean isOrganizer = (toggleGroup.getCheckedButtonId() == R.id.my_events_filter_button);

        EventViewFragment eventViewFragment =
                EventViewFragment.newInstance(clickedEvent, isOrganizer);

        // The transaction code remains the same
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, eventViewFragment)
                    .addToBackStack(null)
                    .commit();
        }
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

    /**
     * This function handles the logic for when the user presses "Delete Account" on the dialog.
     * It calls deleteProfile on the ProfileViewModel, and then if that succeeds, it navigates back to the login.
     * A toast is also displayed for success and error.
     */
    public void confirmAccountDelete() {
        ProfileModel profileToDelete = userProfile;

        adminRepository.deleteUser(profileToDelete, new AdminRepository.DeleteCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "User deleted", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(UserFragment.this)
                        .navigate(R.id.action_user_to_browse_users);
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to delete user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
