package com.rocket.radar;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that displays a list of notifications for the current user.
 * It uses a NotificationRepository for data and a NotificationAdapter for display.
 * Author: Braden Woods
 */
public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private static final String TAG = "NotificationFragment";

    // UI Components
    private RecyclerView notificationsRecyclerView;
    private TextView emptyNotificationsText;

    // Adapter and Data
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    // Data Layer
    private NotificationRepository notificationRepository;
    private ListenerRegistration notificationListenerRegistration;

    /**
     * Default empty constructor REQUIRED for the Android Framework and for production use.
     * The fragment will create its own repository instance.
     */
    public NotificationFragment() {
        // This constructor MUST be public and empty.
    }

    /**
     * Constructor for testing purposes.
     * This allows a test class to inject a pre-configured NotificationRepository.
     * @param notificationRepository The repository to use for data operations.
     */
    public NotificationFragment(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_list, container, false);

        // --- DEPENDENCY INJECTION LOGIC ---
        // If the repository was not injected by a test, create a new one now for production use.
        if (this.notificationRepository == null) {
            this.notificationRepository = new NotificationRepository();
        }

        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view);
        emptyNotificationsText = view.findViewById(R.id.empty_notifications_text);
        Button backButton = view.findViewById(R.id.back_arrow);

        setupRecyclerView();

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRealtimeListener();
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, this);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationsRecyclerView.setAdapter(adapter);
    }

    private void setupRealtimeListener() {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences("YourAppPrefs", Context.MODE_PRIVATE);
        String currentUserId = prefs.getString("USER_ID_KEY", "none");

        if ("none".equals(currentUserId)) {
            Log.w(TAG, "No user ID found, cannot set up listener.");
            updateUI();
            return;
        }

        Log.d(TAG, "Setting up real-time listener for user: " + currentUserId);

        notificationListenerRegistration = notificationRepository.addSnapshotListenerForUserNotifications(currentUserId,
                (snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        notificationList.clear();
                        if (emptyNotificationsText != null) {
                            emptyNotificationsText.setText(R.string.failed_to_load_notifications);
                        }
                        updateUI();
                        return;
                    }

                    if (snapshot != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot document : snapshot) {
                            Notification notification = document.toObject(Notification.class);
                            notification.setId(document.getId());
                            notificationList.add(notification);
                        }
                        Log.d(TAG, "Snapshot updated. Found " + notificationList.size() + " notifications.");
                        updateUI();
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
            Log.d(TAG, "Notification listener removed.");
        }
    }

    @Override
    public void onNotificationClicked(Notification notification, int position) {
        if (notification == null || getContext() == null) return;

        Log.d(TAG, "Notification clicked: " + notification.getTitle());

        String currentUserId = requireContext().getSharedPreferences("YourAppPrefs", Context.MODE_PRIVATE)
                .getString("USER_ID_KEY", "none");

        if (!"none".equals(currentUserId) && !notification.isRead()) {
            notificationRepository.markNotificationAsRead(currentUserId, notification.getId())
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully marked as read in Firestore.");
                        notification.setRead(true);
                        adapter.notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification as read.", e));
        }

        switch (notification.getTypeEnum()) {
            case SELECTED_ENTRANTS:
                RsvpDialogFragment rsvpDialog = new RsvpDialogFragment();
                rsvpDialog.show(getParentFragmentManager(), "RsvpDialog");
                break;

            case EVENT_DETAILS:
            case WAITLIST_STATUS:
            case GENERIC_MESSAGE:
            default:
                NotificationInfoDialogFragment infoDialog = NotificationInfoDialogFragment.newInstance(
                        notification.getTitle(),
                        notification.getDescription()
                );
                infoDialog.show(getParentFragmentManager(), "InfoDialog");
                break;
        }
    }

    private void updateUI() {
        if (notificationList == null || notificationList.isEmpty()) {
            notificationsRecyclerView.setVisibility(View.GONE);
            emptyNotificationsText.setVisibility(View.VISIBLE);
        } else {
            notificationsRecyclerView.setVisibility(View.VISIBLE);
            emptyNotificationsText.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        }
    }
}
