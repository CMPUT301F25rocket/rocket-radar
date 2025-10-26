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
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment that displays a list of notifications for the current user.
 * It follows a modern Android architecture:
 * - It uses a {@link NotificationRepository} to abstract data operations.
 * - It displays the data in a {@link RecyclerView} via a {@link NotificationAdapter}.
 * - It handles user clicks by implementing the adapter's {@link NotificationAdapter.OnNotificationClickListener} interface.
 * <p>
 * Author: Braden Woods
 */
public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    // A tag for logging, which is useful for debugging.
    private static final String TAG = "NotificationFragment";

    // UI Components
    private RecyclerView notificationsRecyclerView;
    private TextView emptyNotificationsText;

    // Adapter and Data Source
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    // Data Layer
    private NotificationRepository notificationRepository;
    private ListenerRegistration notificationListenerRegistration;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where you inflate the layout and find view by ID.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.notification_list, container, false);

        // Initialize the repository that will handle all Firestore communication.
        notificationRepository = new NotificationRepository();

        // Find UI elements from the inflated layout.
        notificationsRecyclerView = view.findViewById(R.id.notifications_recycler_view);
        emptyNotificationsText = view.findViewById(R.id.empty_notifications_text);
        Button backButton = view.findViewById(R.id.back_arrow);

        // Set up the RecyclerView with its adapter and layout manager.
        setupRecyclerView();

        // Set up the listener for the back button to navigate away from this fragment.
        backButton.setOnClickListener(v -> {
            // A null check on getActivity() is good practice.
            if (getActivity() != null) {
                // popBackStack() is the standard way to close a fragment that was added to the back stack.
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

    /**
     * Called immediately after onCreateView() has returned.
     * This is a good place to kick off data loading.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Start listening for real-time notification updates.
        setupRealtimeListener();
    }

    /**
     * Initializes the RecyclerView, its LayoutManager, and the NotificationAdapter.
     */
    private void setupRecyclerView() {
        // Initialize the data list.
        notificationList = new ArrayList<>();
        // Create the adapter, passing the data list and 'this' as the click listener.
        adapter = new NotificationAdapter(notificationList, this);
        // Set the layout manager that the RecyclerView will use to position items.
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Attach the adapter to the RecyclerView.
        notificationsRecyclerView.setAdapter(adapter);
    }

    /**
     * Sets up a real-time listener for the current user's notifications.
     * The list will automatically update whenever the data changes in Firestore.
     */
    private void setupRealtimeListener() {
        if (getContext() == null) return;

        // Retrieve the current user's ID from SharedPreferences.
        // TODO: Replace "YourAppPrefs" and "USER_ID_KEY" with centralized constants.
        SharedPreferences prefs = requireContext().getSharedPreferences("YourAppPrefs", Context.MODE_PRIVATE);
        String currentUserId = prefs.getString("USER_ID_KEY", "none");

        // If no user is logged in, clear the list and show the empty state.
        if ("none".equals(currentUserId)) {
            Log.w(TAG, "No user ID found, cannot set up listener.");
            notificationList.clear();
            updateUI();
            return;
        }

        Log.d(TAG, "Setting up real-time listener for user: " + currentUserId);

        // Use the new repository method. The lambda here is the EventListener callback.
        notificationListenerRegistration = notificationRepository.addSnapshotListenerForUserNotifications(currentUserId,
                (snapshot, error) -> {
                    // --- Handle potential errors ---
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        notificationList.clear();
                        if (emptyNotificationsText != null) {
                            emptyNotificationsText.setText(R.string.failed_to_load_notifications);
                        }
                        updateUI();
                        return;
                    }

                    // --- Handle successful data snapshot ---
                    if (snapshot != null) {
                        notificationList.clear(); // Clear the list to refresh it.
                        for (QueryDocumentSnapshot document : snapshot) {
                            Notification notification = document.toObject(Notification.class);
                            notification.setId(document.getId());
                            notificationList.add(notification);
                        }
                        Log.d(TAG, "Snapshot updated. Found " + notificationList.size() + " notifications.");
                        // After processing the snapshot, refresh the UI.
                        updateUI();
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                });
    }

    /**
     * Called when the fragment is being destroyed.
     * This is the correct place to clean up resources, especially listeners.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // IMPORTANT: Remove the Firestore listener to prevent memory leaks.
        // If we don't do this, the listener will stay active even after the fragment is gone.
        if (notificationListenerRegistration != null) {
            notificationListenerRegistration.remove();
            Log.d(TAG, "Notification listener removed.");
        }
    }

    /**
     * The required implementation of the OnNotificationClickListener interface from the adapter.
     * This method is called whenever a user taps on an item in the RecyclerView.
     *
     * @param notification The Notification object associated with the clicked item.
     * @param position The adapter position of the clicked item.
     */
    @Override
    public void onNotificationClicked(Notification notification, int position) {
        if (notification == null || getContext() == null) return;

        Log.d(TAG, "Notification clicked at position " + position + ": " + notification.getTitle());

        // --- Mark the notification as read ---
        String currentUserId = requireContext().getSharedPreferences("YourAppPrefs", Context.MODE_PRIVATE)
                .getString("USER_ID_KEY", "none");

        // Only mark as read if the notification is currently unread.
        if (!"none".equals(currentUserId) && !notification.isRead()) {
            Log.d(TAG, "Marking notification as read: " + notification.getId());
            notificationRepository.markNotificationAsRead(currentUserId, notification.getId())
                    .addOnSuccessListener(aVoid -> {
                        // On success, update the UI locally for an instant visual change.
                        Log.d(TAG, "Successfully marked as read in Firestore.");
                        notification.setRead(true);
                        // Instead of redrawing the whole list, just update the single item that changed.
                        // This is much more efficient and gives a smoother user experience.
                        adapter.notifyItemChanged(position);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to mark notification as read.", e));
        }

        // --- Handle the click action based on the notification's type ---
        switch (notification.getTypeEnum()) {
            case SELECTED_ENTRANTS:
                // TODO: The RsvpDialogFragment needs to be implemented.
                // It should likely take the eventId as an argument.
                RsvpDialogFragment rsvpDialog = new RsvpDialogFragment();
                rsvpDialog.show(getParentFragmentManager(), "RsvpDialog");
                break;

            case EVENT_DETAILS:
            case WAITLIST_STATUS:
            case GENERIC_MESSAGE: // Explicitly handle the default case.
            default:
                // For all other types, show a generic information dialog.
                NotificationInfoDialogFragment infoDialog = NotificationInfoDialogFragment.newInstance(
                        notification.getTitle(),
                        notification.getDescription()
                );
                infoDialog.show(getParentFragmentManager(), "InfoDialog");
                break;
        }
    }

    /**
     * Updates the UI to show either the notification list or the "empty" text view.
     * This should be called any time the data in `notificationList` changes.
     */
    private void updateUI() {
        if (notificationList == null || notificationList.isEmpty()) {
            // If the list is empty, hide the RecyclerView and show the placeholder text.
            notificationsRecyclerView.setVisibility(View.GONE);
            emptyNotificationsText.setVisibility(View.VISIBLE);
        } else {
            // If the list has data, show the RecyclerView and hide the placeholder text.
            notificationsRecyclerView.setVisibility(View.VISIBLE);
            emptyNotificationsText.setVisibility(View.GONE);
            // Tell the adapter that the data has changed so it can re-render the list.
            adapter.notifyDataSetChanged();
        }
    }
}
