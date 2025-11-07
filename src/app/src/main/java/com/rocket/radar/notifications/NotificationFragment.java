package com.rocket.radar.notifications;

/**
 * A {@link Fragment} that displays a list of notifications for the current user.
 * It uses a {@link RecyclerView} to present the data and communicates with a
 * {@link NotificationRepository} to fetch and observe notification data from Firestore.
 *
 * This fragment manages the UI state, showing a "no notifications" message when the list
 * is empty. It also handles navigation, allowing the user to return to the previous screen.
 * The sorting of notifications (unread first, then by date) is handled within this class.
 */

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView; // Make sure this is imported
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;
import java.util.ArrayList;

public class NotificationFragment extends Fragment {

    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView; // We need this again
    private Button backButton;

    private NotificationAdapter adapter;
    private NotificationRepository notificationRepository;
    private RecyclerView.AdapterDataObserver adapterObserver; // Declare the observer

    /**
     * Inflates the fragment's layout and initializes view and repository instances.
     * This method is called to create the view hierarchy associated with the fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_list, container, false);

        notificationRepository = new NotificationRepository();

        // Find all the views again
        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        backButton = view.findViewById(R.id.back_arrow);

        return view;
    }

    /**
     * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
     * but before any saved state has been restored in to the view. This is where final view
     * initialization, such as setting up RecyclerView, click listeners, and observers, should occur.
     *
     * @param view The View returned by {@link #onCreateView}.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupClickListeners();
        observeNotifications();
    }

    /**
     * Configures the RecyclerView, its layout manager, and the custom adapter.
     * It also sets up a {@link RecyclerView.AdapterDataObserver} to monitor changes in the
     * adapter's data set and toggle the visibility of the empty state view.
     */
    private void setupRecyclerView() {
        // Use the original adapter that does NOT handle the empty state
        adapter = new NotificationAdapter(getContext(), new ArrayList<>(), notificationRepository);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);

        // Create an observer that will react to any changes in the adapter's data.
        adapterObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }

            void checkEmpty() {
                // This is the guaranteed correct way to check if the adapter is empty.
                if (adapter.getItemCount() == 0) {
                    emptyNotificationsTextView.setVisibility(View.VISIBLE);
                    notificationRecyclerView.setVisibility(View.GONE);
                } else {
                    emptyNotificationsTextView.setVisibility(View.GONE);
                    notificationRecyclerView.setVisibility(View.VISIBLE);
                }
            }
        };

        // Register the observer with the adapter.
        adapter.registerAdapterDataObserver(adapterObserver);

        // Perform an initial check in case the LiveData is already populated
        checkEmpty();
    }

    /**
     * Sets up the click listener for the back button, which pops the back stack to
     * return the user to the previous screen.
     */
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
        });
    }

    /**
     * Subscribes to the notification data stream from the {@link NotificationRepository}.
     * When new data is received, it sorts the notifications (unread first, then by date)
     * and updates the adapter.
     */
    private void observeNotifications() {
        notificationRepository.getMyNotifications().observe(getViewLifecycleOwner(), newNotifications -> {
            Log.d("NotificationFragment", "Data updated. " + newNotifications.size() + " notifications received.");

            newNotifications.sort((n1, n2) -> {
                int readCompare = Boolean.compare(n1.isReadStatus(), n2.isReadStatus());
                if (readCompare != 0) { return readCompare; }
                if (n1.getTimestamp() != null && n2.getTimestamp() != null) {
                    return n2.getTimestamp().compareTo(n1.getTimestamp());
                }
                return 0;
            });

            // The registered observer will handle showing/hiding the empty view automatically.
            adapter.setNotifications(newNotifications);
        });
    }

    /**
     * Checks if the adapter is empty and updates the visibility of the RecyclerView and
     * the empty state TextView accordingly. This provides a user-friendly message when
     * there are no notifications to display.
     */
    private void checkEmpty() {
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
     * Called when the view hierarchy associated with the fragment is being removed.
     * This method is responsible for cleaning up resources, such as unregistering the
     * {@link RecyclerView.AdapterDataObserver} to prevent memory leaks and restoring
     * the visibility of the main activity's bottom navigation.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // It's crucial to unregister the observer to prevent memory leaks.
        if (adapter != null && adapterObserver != null) {
            adapter.unregisterAdapterDataObserver(adapterObserver);
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }

    /**
     * Called when the fragment is visible to the user and actively running.
     * This implementation ensures the bottom navigation bar is hidden while the
     * notification screen is displayed, providing more screen real estate.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}
