package com.rocket.radar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
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
import com.rocket.radar.profile.ProfileModel;

import java.util.ArrayList;
import java.util.List;

public class OrganizerEntrantsFragment extends Fragment implements OnMapReadyCallback {

    // THIS WORKS SO FAR LFG

    private GoogleMap googleMap;
    private BottomSheetBehavior<MaterialCardView> bottomSheetBehavior;

    // UI elements from the layout
    private LinearLayout attendingActions;
    private LinearLayout cancelledActions;
    private MaterialCardView sendNotificationDialog;
    private View dialogScrim;

    // Add variables for RecyclerView and Adapter
    private RecyclerView entrantsRecyclerView;
    private EntrantAdapter entrantAdapter;
    private List<ProfileModel> entrantList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_entrants_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_container);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Setup UI components and listeners
        setupBottomSheet(view);
        setupTabs(view);
        setupButtons(view);
        setupDialog(view);
        setupRecyclerView(view); // Add this call
    }

    /**
     * Configures the RecyclerView and its adapter.
     */
    private void setupRecyclerView(View view) {
        entrantsRecyclerView = view.findViewById(R.id.entrants_recycler_view);
        entrantAdapter = new EntrantAdapter(getContext(), entrantList);
        entrantsRecyclerView.setAdapter(entrantAdapter);
        entrantsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // TODO: Replace this with actual data loading from Firebase/Firestore
        loadSampleData();
    }

    /**
     * Loads sample data into the entrant list for demonstration purposes.
     * Replace this with your actual data fetching logic.
     */
    private void loadSampleData() {
        entrantList.clear(); // Clear existing data
        // The ProfileModel constructor you provided needs all arguments.
        // We'll pass null for the ones we don't care about for this sample.
        entrantList.add(new ProfileModel("uid1", "Alice", "alice@example.com", null, null));
        entrantList.add(new ProfileModel("uid2", "Bob", "bob@example.com", null, null));
        entrantList.add(new ProfileModel("uid3", "Charlie", "charlie@example.com", null, null));
        entrantAdapter.notifyDataSetChanged(); // Refresh the list
    }

    // ... (The rest of your OrganizerEntrantsFragment methods remain the same)

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        LatLng edmonton = new LatLng(53.5461, -113.4938);
        googleMap.addMarker(new MarkerOptions().position(edmonton).title("Marker in Edmonton"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(edmonton, 10f));
    }

    private void setupBottomSheet(View view) {
        MaterialCardView bottomSheet = view.findViewById(R.id.bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(300);
    }

    private void setupTabs(View view) {
        TabLayout tabs = view.findViewById(R.id.entrants_filter_tabs);
        attendingActions = view.findViewById(R.id.attending_actions);
        cancelledActions = view.findViewById(R.id.cancelled_actions);

        if (tabs.getTabCount() > 2) {
            updateActionButtons(tabs.getTabAt(2));
        }

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateActionButtons(tab);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) { /* No action */ }
            @Override
            public void onTabReselected(TabLayout.Tab tab) { /* No action */ }
        });
    }

    private void updateActionButtons(TabLayout.Tab tab) {
        attendingActions.setVisibility(View.GONE);
        cancelledActions.setVisibility(View.GONE);

        if (tab != null && tab.getText() != null) {
            String tabText = tab.getText().toString();
            switch (tabText) {
                case "Attending":
                    attendingActions.setVisibility(View.VISIBLE);
                    break;
                case "Cancelled":
                    cancelledActions.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    private void setupButtons(View view) {
        Button backButton = view.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            FragmentActivity activity = getActivity();
            if (activity != null) {
                activity.getOnBackPressedDispatcher().onBackPressed();
            }
        });

        Button sendNotificationButton = view.findViewById(R.id.send_notification_button);
        sendNotificationButton.setOnClickListener(v -> showSendNotificationDialog(true));
    }

    private void setupDialog(View view) {
        sendNotificationDialog = view.findViewById(R.id.send_notification_dialog);
        dialogScrim = view.findViewById(R.id.dialog_scrim);
        Button cancelNotificationButton = view.findViewById(R.id.cancel_notification_button);

        dialogScrim.setOnClickListener(v -> showSendNotificationDialog(false));
        cancelNotificationButton.setOnClickListener(v -> showSendNotificationDialog(false));
    }

    private void showSendNotificationDialog(boolean show) {
        sendNotificationDialog.setVisibility(show ? View.VISIBLE : View.GONE);
        dialogScrim.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
