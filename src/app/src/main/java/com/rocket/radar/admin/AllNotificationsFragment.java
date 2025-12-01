package com.rocket.radar.admin;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.notifications.NotificationAdapter;
import com.rocket.radar.notifications.NotificationRepository;

import java.util.ArrayList;

/**
 * AllNotificationsFragment displays all notifications for the admin to view.
 * This is heavily based on the notification fragment in the package, but all notifications in the app get shown to the admin.
 */
public class AllNotificationsFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView;
    private Button backButton;
    private TextView headerTextView; // Optional: shows notification count

    private NotificationAdapter adapter;
    private NotificationRepository notificationRepository;
    private RecyclerView.AdapterDataObserver adapterObserver;

    /**
     * Creates the view hierarchy for this fragment.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_list, container, false);

        notificationRepository = new NotificationRepository();

        // Initialize views
        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        backButton = view.findViewById(R.id.back_arrow);

        return view;
    }

    /**
     * Called after onCreateView to set up RecyclerView, listeners, and observers.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupClickListeners();
        observeNotifications();
    }

    /**
     * Configures the RecyclerView with the adapter and layout manager,
     * and sets up an observer to handle empty state visibility.
     */
    private void setupRecyclerView() {
        EventRepository eventRepository = new EventRepository();

        adapter = new NotificationAdapter(
                getContext(),
                new ArrayList<>(),
                notificationRepository,
                eventRepository
        );

        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);

        // Create observer to track adapter changes and update empty state
        adapterObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                updateEmptyState();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                updateEmptyState();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                updateEmptyState();
            }
        };

        adapter.registerAdapterDataObserver(adapterObserver);
        updateEmptyState(); // Initial check
    }

    /**
     * Sets up the back button to navigate to the previous fragment.
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    /**
     * Observes ALL notifications from the global notifications collection and updates the adapter.
     * Notifications are already sorted by timestamp (newest first) from Firestore query.
     */
    private void observeNotifications() {
        notificationRepository.getAllNotifications().observe(getViewLifecycleOwner(), allNotifications -> {
            Log.d("AllNotificationsFragment", "Received " + allNotifications.size() + " notifications from global collection");

            // Notifications are already sorted by timestamp from the query
            // No additional sorting needed

            // Update adapter with all notifications
            adapter.setNotifications(allNotifications);
            updateHeaderCount(allNotifications.size());
        });
    }

    /**
     * Updates the visibility of the empty state and RecyclerView.
     */
    private void updateEmptyState() {
        if (adapter != null && emptyNotificationsTextView != null && notificationRecyclerView != null) {
            if (adapter.getItemCount() == 0) {
                emptyNotificationsTextView.setVisibility(View.VISIBLE);
                notificationRecyclerView.setVisibility(View.GONE);
            } else {
                emptyNotificationsTextView.setVisibility(View.GONE);
                notificationRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Updates the header to show the count of notifications.
     * This is optional and can be removed if not needed.
     */
    private void updateHeaderCount(int count) {
        if (headerTextView != null) {
            if (count == 0) {
                headerTextView.setText("No Notifications");
            } else if (count == 1) {
                headerTextView.setText("1 Notification");
            } else {
                headerTextView.setText(count + " Notifications");
            }
        }
    }

    /**
     * Called when the fragment's view is being destroyed.
     * Unregisters observers to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null && adapterObserver != null) {
            adapter.unregisterAdapterDataObserver(adapterObserver);
        }
        // Restore bottom navigation visibility
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }

    /**
     * Called when the fragment is visible to the user.
     * Hides the bottom navigation to maximize screen space.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}