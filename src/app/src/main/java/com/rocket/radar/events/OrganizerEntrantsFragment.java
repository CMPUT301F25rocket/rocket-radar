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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.rocket.radar.R;
import com.rocket.radar.notifications.NotificationRepository;
import com.rocket.radar.profile.ProfileModel;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEntrantsFragment extends Fragment implements OnMapReadyCallback {

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
    private List<ProfileModel> entrantList = new ArrayList<>();

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
        setupActionBars(view);
        setupTabs(view);
        setupButtons(view);
        setupDialog(view);
        setupRecyclerView(view);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        // The map is now only responsible for displaying data.
        // It should not request permissions or fetch the organizer's location.
        LatLng edmonton = new LatLng(53.5461, -113.4938);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(edmonton, 10f));

        // TODO: In the future, we will add a method here like `fetchAndDisplayCheckInLocations()`
        // This method will query Firestore for check-in data and add markers to the map.
    }

    // All location permission and fetching logic has been removed.
    // The rest of the file (setupTabs, setupButtons, etc.) remains the same.
    // ... (rest of the file is unchanged from the previous version, so it's omitted for brevity)

    private void setupRecyclerView(View view) {
        entrantsRecyclerView = view.findViewById(R.id.entrants_recycler_view);
        entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // TODO: Initialize adapter once EntrantAdapter.java is created
        // entrantAdapter = new EntrantAdapter(getContext(), entrantList);
        // entrantsRecyclerView.setAdapter(entrantAdapter);
        loadSampleData();
    }

    private void loadSampleData() {
        entrantList.clear();
        // CORRECTED CONSTRUCTOR USAGE: Now matches the ProfileModel constructor.
        // We pass null for phoneNumber and lastLogin, and default booleans.
        entrantList.add(new ProfileModel("uid1", "Alice", "alice@example.com", null, null, true, true, false));
        entrantList.add(new ProfileModel("uid2", "Bob", "bob@example.com", null, null, true, true, false));
        // if (entrantAdapter != null) entrantAdapter.notifyDataSetChanged();
    }

    private void setupBottomSheet(View view) {
        MaterialCardView bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(getResources().getDisplayMetrics().heightPixels / 4);
    }

    private void setupActionBars(View view) {
        waitlistActions = view.findViewById(R.id.waitlist_actions);
        invitedActions = view.findViewById(R.id.invited_actions);
        attendingActions = view.findViewById(R.id.attending_actions);
        cancelledActions = view.findViewById(R.id.cancelled_actions);
    }

    private void setupTabs(View view) {
        tabs = view.findViewById(R.id.entrants_filter_tabs);
        updateActionButtons(tabs.getTabAt(0)); // Set initial state

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

        // Set up listeners for all "Send Notification" buttons
        View.OnClickListener openDialogListener = v -> showSendNotificationDialog(true);
        view.findViewById(R.id.waitlist_send_notification_button).setOnClickListener(openDialogListener);
        view.findViewById(R.id.invited_send_notification_button).setOnClickListener(openDialogListener);
        view.findViewById(R.id.attending_send_notification_button).setOnClickListener(openDialogListener);
        view.findViewById(R.id.cancelled_send_notification_button).setOnClickListener(openDialogListener);

        // TODO: Add listeners for other buttons (Export CSV, Cancel Invitation, etc.)
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

            // Determine which group to send to based on the currently selected tab
            String groupField = getGroupFieldForCurrentTab();
            if (groupField != null) {
                notificationRepository.sendNotificationToGroup(title, body, event.getEventId(), groupField);
                Toast.makeText(getContext(), "Notification sent to " + tabs.getTabAt(tabs.getSelectedTabPosition()).getText(), Toast.LENGTH_SHORT).show();
                showSendNotificationDialog(false); // Hide dialog after sending
            } else {
                Toast.makeText(getContext(), "Could not determine target group.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSendNotificationDialog(boolean show) {
        sendNotificationDialog.setVisibility(show ? View.VISIBLE : View.GONE);
        dialogScrim.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) {
            // Clear fields when hiding
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
