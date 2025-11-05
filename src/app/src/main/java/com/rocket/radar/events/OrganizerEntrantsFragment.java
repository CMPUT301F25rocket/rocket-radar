package com.rocket.radar.events;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rocket.radar.R;
import com.rocket.radar.notifications.NotificationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrganizerEntrantsFragment extends Fragment implements OnMapReadyCallback, EntrantAdapter.OnEntrantClickListener {

    private static final String ARG_EVENT = "event";
    private static final String TAG = "OrganizerEntrants";

    private GoogleMap googleMap;
    private BottomSheetBehavior<MaterialCardView> bottomSheetBehavior;
    private Event event;
    private NotificationRepository notificationRepository;

    // UI elements
    private LinearLayout waitlistActions, invitedActions, attendingActions, cancelledActions;
    private MaterialCardView sendNotificationDialog;
    private View dialogScrim;
    private EditText notificationTitleInput, notificationBodyInput;
    private TabLayout tabs;
    private RecyclerView entrantsRecyclerView;
    private EntrantAdapter entrantAdapter;
    private List<CheckIn> checkInList = new ArrayList<>();
    private final Map<String, Marker> userMarkers = new HashMap<>(); // Maps userId to Map Marker

    // --- START OF FIX: Add a variable to hold the bottom sheet view ---
    private MaterialCardView bottomSheet;
    // --- END OF FIX ---

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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrants_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // The order is important: setupBottomSheet must be called before setupRecyclerView
        setupBottomSheet(view);
        setupActionBars(view);
        setupTabs(view);
        setupButtons(view);
        setupDialog(view);
        setupRecyclerView(view); // Now this will work correctly
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        LatLng edmonton = new LatLng(53.5461, -113.4938);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(edmonton, 10f));

        // Fetch and display check-in locations as pins
        fetchAndDisplayCheckInLocations();
    }

    private void fetchAndDisplayCheckInLocations() {
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null, cannot fetch check-ins.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events").document(event.getEventId())
                .collection("checkins") // Assumes you create this sub-collection
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && googleMap != null) {
                        googleMap.clear(); // Clear old pins
                        checkInList.clear();
                        userMarkers.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CheckIn checkIn = document.toObject(CheckIn.class);
                            checkInList.add(checkIn);

                            GeoPoint geoPoint = checkIn.getSignupLocation();
                            if (geoPoint != null) {
                                LatLng position = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                Marker marker = googleMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(checkIn.getUserName()) // Set the user's name as the marker title
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))); // Custom color

                                if (marker != null) {
                                    userMarkers.put(checkIn.getUserId(), marker);
                                }
                            }
                        }
                        entrantAdapter.notifyDataSetChanged(); // Update the RecyclerView
                        Log.d(TAG, "Fetched and displayed " + checkInList.size() + " check-in locations.");
                    } else {
                        Log.w(TAG, "Error getting check-in documents.", task.getException());
                    }
                });
    }

    @Override
    public void onEntrantClick(CheckIn checkIn) {
        Marker marker = userMarkers.get(checkIn.getUserId());
        if (googleMap != null && marker != null) {
            // Animate the camera to recenter on the clicked user's pin
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f));
            marker.showInfoWindow(); // Show the user's name on the pin
            Toast.makeText(getContext(), "Showing location for " + checkIn.getUserName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Location not available for this user.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- START OF FIX: Corrected RecyclerView setup ---
    private void setupRecyclerView(View view) {
        // Find the RecyclerView *inside* the bottomSheet view
        entrantsRecyclerView = bottomSheet.findViewById(R.id.entrants_recycler_view);

        if (entrantsRecyclerView != null) {
            entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            // Initialize the adapter with the list and this fragment as the listener
            entrantAdapter = new EntrantAdapter(checkInList, this);
            entrantsRecyclerView.setAdapter(entrantAdapter);
        } else {
            // This log helps if the ID is still wrong for some reason
            Log.e(TAG, "CRITICAL: entrants_recycler_view could not be found within the bottom sheet.");
        }
    }
    // --- END OF FIX ---

    // --- START OF FIX: Corrected BottomSheet setup ---
    private void setupBottomSheet(View view) {
        // Find the bottom sheet and assign it to the class variable so other methods can use it
        bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels / 4);
    }
    // --- END OF FIX ---

    private void setupActionBars(View view) {
        waitlistActions = view.findViewById(R.id.waitlist_actions);
        invitedActions = view.findViewById(R.id.invited_actions);
        attendingActions = view.findViewById(R.id.attending_actions);
        cancelledActions = view.findViewById(R.id.cancelled_actions);
    }

    private void setupTabs(View view) {
        tabs = view.findViewById(R.id.entrants_filter_tabs);
        updateActionButtons(tabs.getTabAt(0));

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateActionButtons(tab);
                // TODO: Add logic to filter the map markers and recycler view list based on the selected tab
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { /* No action */ }
            @Override public void onTabReselected(TabLayout.Tab tab) { /* No action */ }
        });
    }

    private void updateActionButtons(TabLayout.Tab tab) {
        waitlistActions.setVisibility(View.GONE);
        invitedActions.setVisibility(View.GONE);
        attendingActions.setVisibility(View.GONE);
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
                attendingActions.setVisibility(View.VISIBLE);
                break;
            case "Cancelled":
                cancelledActions.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void setupButtons(View view) {
        view.findViewById(R.id.back_button).setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        View.OnClickListener openDialogListener = v -> showSendNotificationDialog(true);
        view.findViewById(R.id.waitlist_send_notification_button).setOnClickListener(openDialogListener);
        view.findViewById(R.id.invited_send_notification_button).setOnClickListener(openDialogListener);
        view.findViewById(R.id.attending_send_notification_button).setOnClickListener(openDialogListener);
        view.findViewById(R.id.cancelled_send_notification_button).setOnClickListener(openDialogListener);
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

            String groupField = getGroupFieldForCurrentTab();
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
        sendNotificationDialog.setVisibility(show ? View.VISIBLE : View.GONE);
        dialogScrim.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            notificationTitleInput.setText("");
            notificationBodyInput.setText("");
        }
    }

    private String getGroupFieldForCurrentTab() {
        int selectedTabPosition = tabs.getSelectedTabPosition();
        if (selectedTabPosition == -1) return null;

        TabLayout.Tab tab = tabs.getTabAt(selectedTabPosition);
        if (tab == null || tab.getText() == null) return null;

        switch (tab.getText().toString()) {
            case "On Waitlist":
                return "onWaitlistEventIds";
            case "Attending":
                return "attendees";
            case "Invited":
                return "invited";
            case "Cancelled":
                return "cancelled";
            default:
                return null;
        }
    }
}
