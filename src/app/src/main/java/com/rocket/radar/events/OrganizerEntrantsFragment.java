package com.rocket.radar.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;



import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.lottery.LotteryLogic;
import com.rocket.radar.notifications.NotificationRepository;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import kotlinx.serialization.descriptors.PrimitiveKind;

/**
 * A fragment for event organizers to manage entrants.
 * This screen displays a list of entrants categorized by their status (Waitlisted, Invited, Selected, Cancelled)
 * and a map showing the locations of waitlisted users.
 * Organizers can send notifications to these groups.
 *
 * Outstanding Issues:
 * - The "Attending" tab in the UI should be relabeled to "Selected" to match the data model.
 * - The map currently only shows locations for waitlisted users, could be extended for other statuses if needed.
 */
public class OrganizerEntrantsFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_EVENT = "event";
    private static final String TAG = "OrganizerEntrants";

    private GoogleMap googleMap;
    private BottomSheetBehavior<MaterialCardView> bottomSheetBehavior;

    private static final String TUPLE_NAME_KEY = "name";
    private ListView entrantsListView;
    private SimpleAdapter entrantsAdapter;
    private ArrayList<Map<String, String>> currentEntrants; // Each map is a tuple: {"name": name, "id": id}

    private Event event;
    private EventRepository eventRepository;
    private NotificationRepository notificationRepository;
    private ProfileRepository profileRepository;

    // UI elements
    private LinearLayout waitlistActions, invitedActions, selectedActions, cancelledActions;
    private MaterialCardView sendNotificationDialog;
    private View dialogScrim;
    private EditText notificationTitleInput, notificationBodyInput;
    private TabLayout tabs;

    //private EntrantAdapter entrantAdapter;

    private ArrayList<String> allEntrantsList = new ArrayList<>();
    private ArrayList<String> filteredEntrantsList = new ArrayList<>();

    private final Map<String, Marker> userMarkers = new HashMap<>();
    private MaterialCardView bottomSheet;

    public OrganizerEntrantsFragment() {

    }

    public static OrganizerEntrantsFragment newInstance(Event event) {
        OrganizerEntrantsFragment fragment = new OrganizerEntrantsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
        }
        notificationRepository = new NotificationRepository();
        profileRepository = new ProfileRepository();
        eventRepository = EventRepository.getInstance();

        currentEntrants = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrants_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        entrantsListView = view.findViewById(R.id.entrants_list);
        if (entrantsListView != null) {
            entrantsAdapter = new SimpleAdapter(
                    requireContext(),
                    currentEntrants,
                    android.R.layout.simple_list_item_1,
                    new String[]{TUPLE_NAME_KEY},
                    new int[]{android.R.id.text1}
            );
            entrantsListView.setAdapter(entrantsAdapter);

            // --- START OF FIX: Set the item click listener ---
            entrantsListView.setOnItemClickListener((parent, view1, position, id) -> {
                // Get the user ID from the tuple at the clicked position
                String userId = currentEntrants.get(position).get("id");

                // Call your existing method to handle the logic
                onEntrantListItemClick(userId);
            });
        }
            else {
            Log.e(TAG, "ListView (entrants_list) not found in the layout!");
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fetchAndDisplayWaitlistLocations();
        setupBottomSheet(view);
        setupTabs(view);
        setupActionBars();
        setupButtons(view);
        setupDialog(view);
    }


    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        LatLng edmonton = new LatLng(53.5461, -113.4938);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(edmonton, 10f));
        // fetchAndDisplayCheckInLocations(); // This will now fetch ALL users
    }

    private void fetchAndDisplayWaitlistLocations() {
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null, cannot fetch user locations.");
            return;

        }
        eventRepository.getWaitlistLocations(event.getEventId(), new EventRepository.WaitlistLocationsCallback() {
            @Override
            public void onWaitlistLocationsFetched(List<GeoPoint> locations) {
                if (googleMap == null) return;
                googleMap.clear(); // Clear existing markers before adding new ones
                for (GeoPoint location : locations) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(latLng));
                }
                Log.d(TAG, "Displayed " + locations.size() + " waitlist locations on the map.");
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching waitlist locations", e);
            }
        });

    }

    private void filterAndDisplayEntrants(TabLayout.Tab tab) {
        // 1. Clear the member list. The adapter is already connected to this list.
        currentEntrants.clear();

        String status = getStatusStringForTab(tab);

        if (status != null) {
            switch (status) {
                case "waitlisted":
                    // Check for valid event data before making a network call
                    if (event == null || event.getEventId() == null) {
                        Log.e(TAG, "Event or Event ID is null. Cannot fetch waitlist.");
                        Toast.makeText(getContext(), "Event data is missing.", Toast.LENGTH_SHORT).show();
                        entrantsAdapter.notifyDataSetChanged(); // Ensure the list is shown as empty
                        return; // Stop execution
                    }

                    // Start the asynchronous call to get data from Firestore
                    eventRepository.getWaitlistSize(event, new EventRepository.WaitlistSizeListener() {
                        @Override
                        public void onSizeReceived(int size) {
                            
                        }

                        @Override
                        public void onWaitlistEntrantsFetched(List<String> userIds) {
                            // This code runs when the data is successfully fetched.
                            Log.d(TAG, "Fetched " + userIds.size() + " waitlisted entrants.");
                            if (userIds.isEmpty()) {
                                entrantsAdapter.notifyDataSetChanged(); // Refresh to show an empty list
                                return;
                            }

                            // Create placeholder tuples with just the ID
                            for (String userId : userIds) {
                                Map<String, String> entrantTuple = new HashMap<>();
                                entrantTuple.put("id", userId);
                                entrantTuple.put(TUPLE_NAME_KEY, "Loading..."); // Placeholder name
                                currentEntrants.add(entrantTuple);
                            }
                            entrantsAdapter.notifyDataSetChanged(); // Show loading state

                            // Use an array to make the counter 'final' for use in the inner class
                            final int[] profilesToFetch = {userIds.size()};

                            for (String userId : userIds) {
                                profileRepository.readProfile(userId, new ProfileRepository.ReadCallback() {
                                    @Override
                                    public void onProfileLoaded(ProfileModel profile) {
                                        Log.d(TAG, "Fetched user name: " + profile.getName());
                                        // Find the tuple with this ID and update its name
                                        updateEntrantName(userId, profile.getName());
                                        profilesToFetch[0]--; // Decrement the counter
                                        // If this was the last profile to fetch, update the UI
                                        if (profilesToFetch[0] == 0) {
                                            entrantsAdapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e(TAG, "Error fetching user profile", e);
                                        profilesToFetch[0]--; // Also decrement on error to avoid getting stuck
                                        updateEntrantName(userId, "Error Loading Name");
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error fetching waitlist entrants", e);
                            Toast.makeText(getContext(), "Failed to load waitlist.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    // Do not notify the adapter here. It will be notified inside the callback.
                    break;


                case "invited":
                    // Check for valid event data before making a network call
                    if (event == null || event.getEventId() == null) {
                        Log.e(TAG, "Event or Event ID is null. Cannot fetch invited entrants.");
                        Toast.makeText(getContext(), "Event data is missing.", Toast.LENGTH_SHORT).show();
                        entrantsAdapter.notifyDataSetChanged(); // Ensure the list is shown as empty
                        return; // Stop execution
                    }

                    eventRepository.getInvitedEntrants(event.getEventId(), new EventRepository.InvitedEntrantsCallback() {
                        @Override
                        public void onInvitedEntrantsFetched(List<String> userIds) {
                            Log.d(TAG, "Fetched " + userIds.size() + " invited entrants.");
                            if (userIds.isEmpty()) {
                                entrantsAdapter.notifyDataSetChanged();
                                return;
                            }

                            // Create placeholder tuples
                            for (String userId : userIds) {
                                Map<String, String> entrantTuple = new HashMap<>();
                                entrantTuple.put("id", userId);
                                entrantTuple.put(TUPLE_NAME_KEY, "Loading...");
                                currentEntrants.add(entrantTuple);
                            }
                            entrantsAdapter.notifyDataSetChanged();

                            final int[] profilesToFetch = {userIds.size()};

                            for (String userId : userIds) {
                                profileRepository.readProfile(userId, new ProfileRepository.ReadCallback() {
                                    @Override
                                    public void onProfileLoaded(ProfileModel profile) {
                                        updateEntrantName(userId, profile.getName());
                                        profilesToFetch[0]--;
                                        if (profilesToFetch[0] == 0) {
                                            entrantsAdapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        updateEntrantName(userId, "Error Loading Name");
                                        Log.e(TAG, "Error fetching user profile for invited", e);
                                        profilesToFetch[0]--;
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error fetching invited entrants", e);
                            Toast.makeText(getContext(), "Failed to load invited list.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                case "attending":
                    if (event == null || event.getEventId() == null) {
                        Log.e(TAG, "Event or Event ID is null. Cannot fetch selected entrants.");
                        Toast.makeText(getContext(), "Event data is missing.", Toast.LENGTH_SHORT).show();
                        entrantsAdapter.notifyDataSetChanged();
                        return;
                    }

                    eventRepository.getAttendingEntrants(event.getEventId(), new EventRepository.AttendingEntrantsCallback() {
                        @Override
                        public void AttendingEntrantsFetched(List<String> userIds) {
                            Log.d(TAG, "Fetched " + userIds.size() + " selected entrants.");
                            if (userIds.isEmpty()) {
                                entrantsAdapter.notifyDataSetChanged();
                                return;
                            }

                            for (String userId : userIds) {
                                Map<String, String> entrantTuple = new HashMap<>();
                                entrantTuple.put("id", userId);
                                entrantTuple.put(TUPLE_NAME_KEY, "Loading...");
                                currentEntrants.add(entrantTuple);
                            }
                            entrantsAdapter.notifyDataSetChanged();

                            final int[] profilesToFetch = { userIds.size() };

                            for (String userId : userIds) {
                                profileRepository.readProfile(userId, new ProfileRepository.ReadCallback() {
                                    @Override
                                    public void onProfileLoaded(ProfileModel profile) {
                                        updateEntrantName(userId, profile.getName());
                                        if (--profilesToFetch[0] == 0) {
                                            entrantsAdapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        updateEntrantName(userId, "Error Loading Name");
                                        Log.e(TAG, "Error fetching user profile for selected", e);
                                        profilesToFetch[0]--;
                                    }
                                });
                            }
                        }
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error fetching selected entrants", e);
                            Toast.makeText(getContext(), "Failed to load selected list.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                case "cancelled":
                    if (event == null || event.getEventId() == null) {
                        Log.e(TAG, "Event or Event ID is null. Cannot fetch cancelled entrants.");
                        Toast.makeText(getContext(), "Event data is missing.", Toast.LENGTH_SHORT).show();
                        entrantsAdapter.notifyDataSetChanged();
                        return;
                    }

                    eventRepository.getCancelledEntrants(event.getEventId(), new EventRepository.CancelledEntrantsCallback() {
                        @Override
                        public void onCancelledEntrantsFetched(List<String> userIds) {
                            Log.d(TAG, "Fetched " + userIds.size() + " cancelled entrants.");
                            if (userIds.isEmpty()) {
                                entrantsAdapter.notifyDataSetChanged();
                                return;
                            }

                            for (String userId : userIds) {
                                Map<String, String> entrantTuple = new HashMap<>();
                                entrantTuple.put("id", userId);
                                entrantTuple.put(TUPLE_NAME_KEY, "Loading...");
                                currentEntrants.add(entrantTuple);
                            }
                            entrantsAdapter.notifyDataSetChanged();

                            final int[] profilesToFetch = {userIds.size()};

                            for (String userId : userIds) {
                                profileRepository.readProfile(userId, new ProfileRepository.ReadCallback() {
                                    @Override
                                    public void onProfileLoaded(ProfileModel profile) {
                                        updateEntrantName(userId, profile.getName());
                                        profilesToFetch[0]--;
                                        if (profilesToFetch[0] == 0) {
                                            entrantsAdapter.notifyDataSetChanged();
                                        }
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        updateEntrantName(userId, "Error Loading Name");
                                        Log.e(TAG, "Error fetching user profile for cancelled", e);
                                        profilesToFetch[0]--;
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error fetching cancelled entrants", e);
                            Toast.makeText(getContext(), "Failed to load cancelled list.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    break;

                default:
                    // If no case matches, ensure the list is empty
                    entrantsAdapter.notifyDataSetChanged();
                    break;
            }
        } else {
            // If status is null, ensure the list is empty
            entrantsAdapter.notifyDataSetChanged();
        }
    }

    private void updateEntrantName(String userId, String name) {
        for (Map<String, String> entrant : currentEntrants) {
            if (Objects.equals(entrant.get("id"), userId)) {
                entrant.put(TUPLE_NAME_KEY, name);
                return;
            }
        }
    }

    private String getStatusStringForTab(TabLayout.Tab tab) {
        if (tab == null || tab.getText() == null) return null;
        switch (tab.getText().toString()) {
            case "On Waitlist":
                return "waitlisted";
            case "Invited":
                return "invited";
            case "Attending":
                return "attending";
            case "Cancelled":
                return "cancelled";
            default:
                return null;
        }
    }



    private void onEntrantListItemClick(String userId) {


        eventRepository.getUserLocationFromWaitlist(userId, event.getEventId(), new EventRepository.UserLocationCallback() {

            @Override
            public void onLocationFetched(GeoPoint location) {
                if (googleMap != null && location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f));
                    Toast.makeText(getContext(), "Showing location for " + userId, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Location not available for this user.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error fetching user location for " + userId, e);
                Toast.makeText(getContext(), "Could not retrieve location.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupBottomSheet(View view) {
        bottomSheet = view.findViewById(R.id.bottom_sheet);
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
            bottomSheetBehavior.setHideable(false);
        } else {
            Log.e(TAG, "CRITICAL: bottom_sheet could not be found in the layout.");
        }
    }



    private void setupActionBars() {
        if (bottomSheet == null) return;
        waitlistActions = bottomSheet.findViewById(R.id.waitlist_actions);
        invitedActions = bottomSheet.findViewById(R.id.invited_actions);
        selectedActions = bottomSheet.findViewById(R.id.attending_actions);
        cancelledActions = bottomSheet.findViewById(R.id.cancelled_actions);
    }

    private void setupTabs(View view) {
        tabs = view.findViewById(R.id.entrants_filter_tabs);
        if (tabs != null) {
            tabs.post(() -> {
                updateActionButtons(tabs.getTabAt(tabs.getSelectedTabPosition()));
                // Apply initial filter after layout
                filterAndDisplayEntrants(tabs.getTabAt(tabs.getSelectedTabPosition()));
            });

            tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    updateActionButtons(tab);

                    filterAndDisplayEntrants(tab);

                    if (tab.getText() != null && tab.getText().toString().equals("On Waitlist")) {
                        fetchAndDisplayWaitlistLocations();
                    }

                }
                @Override public void onTabUnselected(TabLayout.Tab tab) { /* No-op */ }
                @Override public void onTabReselected(TabLayout.Tab tab) { /* No-op */ }
            });
        }
    }

    private void setupButtons(View view) {
        view.findViewById(R.id.back_button).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });
        if (bottomSheet == null) return;
        View.OnClickListener openDialogListener = v -> showSendNotificationDialog(true);
        bottomSheet.findViewById(R.id.waitlist_send_notification_button).setOnClickListener(openDialogListener);
        bottomSheet.findViewById(R.id.invited_send_notification_button).setOnClickListener(openDialogListener);
        bottomSheet.findViewById(R.id.attending_send_notification_button).setOnClickListener(openDialogListener); // ID remains attending_...
        bottomSheet.findViewById(R.id.cancelled_send_notification_button).setOnClickListener(openDialogListener);

        view.findViewById(R.id.invited_cancel_button).setOnClickListener(v -> {
            // 1. Get the list of currently invited users (unresponded entrants)
            // We need to fetch them first to know who to cancel.
            eventRepository.getInvitedEntrants(event.getEventId(), new EventRepository.InvitedEntrantsCallback() {
                @Override
                public void onInvitedEntrantsFetched(List<String> userIds) {
                    if (userIds.isEmpty()) {
                        Toast.makeText(getContext(), "No invited users to cancel.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 2. Iterate through the list and cancel each one
                    for (String userId : userIds) {
                        // A. Move from Invited -> Cancelled in Firestore (Event side)
                        eventRepository.addUserToCancelled(event, userId);
                        eventRepository.removeUserFromInvited(event, userId);

                        // B. Update the User's Profile (Client side logic)
                        // We need to update the specific user's profile to reflect the change
                        profileRepository.readProfile(userId, new ProfileRepository.ReadCallback() {
                            @Override
                            public void onProfileLoaded(ProfileModel userProfile) {
                                userProfile.addCancelledEventId(event.getEventId());
                                userProfile.removeInvitedEventId(event.getEventId());
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "Error updating profile for user: " + userId, e);
                            }
                        });
                    }

                    // 3. Automatically re-run the lottery to fill the spots
                    // We re-run for exactly the number of people we just cancelled
                    int spotsFreed = userIds.size();
                    new LotteryLogic(event).handleRunLottery(event, spotsFreed);

                    Toast.makeText(getContext(), "Cancelled " + spotsFreed + " entrants. Lottery re-run.", Toast.LENGTH_SHORT).show();

                    // Refresh the list view
                    filterAndDisplayEntrants(tabs.getTabAt(tabs.getSelectedTabPosition()));
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Error fetching invited entrants for bulk cancellation", e);
                    Toast.makeText(getContext(), "Failed to fetch entrants.", Toast.LENGTH_SHORT).show();
                }
            });
        });

    }

    private void updateActionButtons(TabLayout.Tab tab) {
        if (waitlistActions == null) return;
        waitlistActions.setVisibility(View.GONE);
        invitedActions.setVisibility(View.GONE);
        selectedActions.setVisibility(View.GONE);
        cancelledActions.setVisibility(View.GONE);

        if (tab == null || tab.getText() == null) return;

        switch (tab.getText().toString()) {
            case "On Waitlist":
                waitlistActions.setVisibility(View.VISIBLE);
                break;
            case "Invited":
                invitedActions.setVisibility(View.VISIBLE);
                break;
            case "Attending":
                selectedActions.setVisibility(View.VISIBLE);
                break;
            case "Cancelled":
                cancelledActions.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupDialog(View view) {
        sendNotificationDialog = view.findViewById(R.id.send_notification_dialog);
        dialogScrim = view.findViewById(R.id.dialog_scrim);
        notificationTitleInput = view.findViewById(R.id.notification_title_input);
        notificationBodyInput = view.findViewById(R.id.notification_body_input);
        Button cancelNotificationButton = view.findViewById(R.id.cancel_notification_button);
        Button sendButton = view.findViewById(R.id.send_button);

        dialogScrim.setOnClickListener(v -> showSendNotificationDialog(false));
        cancelNotificationButton.setOnClickListener(v -> showSendNotificationDialog(false));

        sendButton.setOnClickListener(v -> {
            String title = notificationTitleInput.getText().toString().trim();
            String body = notificationBodyInput.getText().toString().trim();
            if (title.isEmpty() || body.isEmpty()) {
                Toast.makeText(getContext(), "Title and message cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (event == null || event.getEventId() == null) {
                Toast.makeText(getContext(), "Error: Event ID is missing.", Toast.LENGTH_SHORT).show();
                return;
            }
            String groupField = getGroupForCurrentTab();
            if (groupField != null) {
                notificationRepository.sendNotificationToGroup(title, body, event.getEventId(), groupField);
                Toast.makeText(getContext(), "Notification sent to " + tabs.getTabAt(tabs.getSelectedTabPosition()).getText(), Toast.LENGTH_SHORT).show();
                showSendNotificationDialog(false);
            } else {
                Toast.makeText(getContext(), "Could not determine target group.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSendNotificationDialog(boolean show) {
        if (sendNotificationDialog == null) return;
        sendNotificationDialog.setVisibility(show ? View.VISIBLE : View.GONE);
        dialogScrim.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            notificationTitleInput.setText("");
            notificationBodyInput.setText("");
        }
    }

    private String getGroupFieldForCurrentTab() {
        if (tabs == null) return null;
        int selectedTabPosition = tabs.getSelectedTabPosition();
        if (selectedTabPosition == -1) return null;
        TabLayout.Tab tab = tabs.getTabAt(selectedTabPosition);
        if (tab == null || tab.getText() == null) return null;
        switch (tab.getText().toString()) {
            case "On Waitlist": return "onWaitlistEventIds";
            case "Attending": return "attendees";
            case "Invited": return "invited";
            case "Cancelled": return "cancelled";
            default: return null;
        }
    }

    private String getGroupForCurrentTab() {
        if (tabs == null) return null;
        int selectedTabPosition = tabs.getSelectedTabPosition();
        if (selectedTabPosition == -1) return null;
        TabLayout.Tab tab = tabs.getTabAt(selectedTabPosition);
        if (tab == null || tab.getText() == null) return null;
        switch (tab.getText().toString()) {
            case "On Waitlist": return "waitlistedUsers";
            case "Attending": return "attendingUsers";
            case "Invited": return "invitedUsers";
            case "Cancelled": return "cancelledUsers";
            default: return null;
        }
    }
}
