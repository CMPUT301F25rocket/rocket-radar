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
import android.widget.ListAdapter;
import android.widget.Toast;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;



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
import com.rocket.radar.MainActivity; // Import MainActivity
import com.rocket.radar.R;
import com.rocket.radar.notifications.NotificationRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private EntrantAdapter entrantAdapter;

    // --- START OF CHANGE: Separate lists for all entrants and filtered entrants ---
    private ArrayList<String> allEntrantsList = new ArrayList<>();
    private ArrayList<String> filteredEntrantsList = new ArrayList<>();
    // --- END OF CHANGE ---

    private final Map<String, Marker> userMarkers = new HashMap<>();
    private MaterialCardView bottomSheet;

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
        fetchAndDisplayCheckInLocations(); // This will now fetch ALL users
    }

    private void fetchAndDisplayCheckInLocations() {
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null, cannot fetch check-ins.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("events").document(event.getEventId())
                .collection("checkins")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allEntrantsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            CheckIn checkIn = document.toObject(CheckIn.class);
                            // Store every fetched entrant
                            allEntrantsList.add(checkIn);
                        }
                        Log.d(TAG, "Fetched " + allEntrantsList.size() + " total entrants.");

                        // After fetching, apply the initial filter based on the current tab
                        if (tabs != null) {
                            filterAndDisplayEntrants(tabs.getTabAt(tabs.getSelectedTabPosition()));
                        }

                    } else {
                        Log.w(TAG, "Error getting check-in documents.", task.getException());
                    }
                });
    }

    // --- START OF NEW METHOD: Filters and updates the UI ---
    private void filterAndDisplayEntrants(TabLayout.Tab tab) {
        if (tab == null || allEntrantsList.isEmpty()) {
            return;
        }

        String statusToFilterBy = getStatusStringForTab(tab);
        filteredEntrantsList.clear();

        // Filter the allEntrantsList into the filteredEntrantsList
        if (statusToFilterBy != null) {
            for (CheckIn checkIn : allEntrantsList) {
                if (statusToFilterBy.equals(checkIn.getStatus())) {
                    filteredEntrantsList.add(checkIn);
                }
            }
        }

        // Notify the adapter that the data has changed
        if (entrantAdapter != null) {
            entrantAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "Filtered and displayed " + filteredEntrantsList.size() + " users for status: " + statusToFilterBy);
    }
    // --- END OF NEW METHOD ---

    private String getStatusStringForTab(TabLayout.Tab tab) {
        if (tab == null || tab.getText() == null) return null;
        switch (tab.getText().toString()) {
            case "On Waitlist": return "waitlisted";
            case "Invited": return "invited";
            case "Attending": return "attending";
            case "Cancelled": return "cancelled";
            default: return null;
        }
    }

    @Override
    public void onEntrantClick(CheckIn checkIn) {
        Marker marker = userMarkers.get(checkIn.getUserId());
        if (googleMap != null && marker != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 15f));
            marker.showInfoWindow();
            Toast.makeText(getContext(), "Showing location for " + checkIn.getUserName(), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "Location not available for this user.", Toast.LENGTH_SHORT).show();
        }
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
        attendingActions = bottomSheet.findViewById(R.id.attending_actions);
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
                    // --- START OF CHANGE: Re-filter the list when a new tab is selected ---
                    filterAndDisplayEntrants(tab);
                    // --- END OF CHANGE ---
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
        bottomSheet.findViewById(R.id.attending_send_notification_button).setOnClickListener(openDialogListener);
        bottomSheet.findViewById(R.id.cancelled_send_notification_button).setOnClickListener(openDialogListener);
    }

    private void updateActionButtons(TabLayout.Tab tab) {
        if (waitlistActions == null) return;
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
}
