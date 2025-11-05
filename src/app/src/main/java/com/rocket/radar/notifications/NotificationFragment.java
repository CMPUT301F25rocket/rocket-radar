package com.rocket.radar.notifications;

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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView;
    private Button backButton;
    private FloatingActionButton addTestNotificationButton;

    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private NotificationRepository notificationRepository;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_list, container, false);

        // Initialize Repository and Data List
        notificationRepository = new NotificationRepository();
        notificationList = new ArrayList<>();

        // Find Views
        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        backButton = view.findViewById(R.id.back_arrow);
        // Ensure you have a FloatingActionButton with this ID in your notification_list.xml
        addTestNotificationButton = view.findViewById(R.id.add_test_notification_button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupRecyclerView();
        setupClickListeners();
        observeNotifications();
    }

    private void setupRecyclerView() {
        // CRITICAL: Pass the repository instance to the adapter
        adapter = new NotificationAdapter(getContext(), notificationList, notificationRepository);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Use this button to test creating new notifications
        addTestNotificationButton.setOnClickListener(v -> {
            Log.d("NotificationFragment", "Add test notification button clicked!");
            notificationRepository.createTestNotificationForCurrentUser("New Test Event", "A new event was just posted!");
        });
    }

    private void observeNotifications() {
        notificationRepository.getMyNotifications().observe(getViewLifecycleOwner(), newNotifications -> {
            Log.d("NotificationFragment", "Data updated. " + newNotifications.size() + " notifications received.");

            // Sort the list: unread first, then by newest timestamp
            newNotifications.sort((n1, n2) -> {
                int readCompare = Boolean.compare(n1.isReadStatus(), n2.isReadStatus());
                if (readCompare != 0) {
                    return readCompare; // false (unread) comes before true (read)
                }
                if (n1.getTimestamp() != null && n2.getTimestamp() != null) {
                    return n2.getTimestamp().compareTo(n1.getTimestamp()); // Newest first
                }
                return 0;
            });

            // Update the adapter with the new, sorted list
            adapter.setNotifications(newNotifications);

            // Update the visibility of the "empty" text view
            updateEmptyViewVisibility();
        });
    }

    private void updateEmptyViewVisibility() {
        // Check the underlying list that the adapter uses
        if (notificationList.isEmpty()) {
            emptyNotificationsTextView.setVisibility(View.VISIBLE);
            notificationRecyclerView.setVisibility(View.GONE);
        } else {
            emptyNotificationsTextView.setVisibility(View.GONE);
            notificationRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}

