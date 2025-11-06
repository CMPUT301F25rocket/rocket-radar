package com.rocket.radar.notifications;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        setupClickListeners();
        observeNotifications();
    }

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

        // --- IMPORTANT ---
        // Perform an initial check in case the LiveData is already populated
        checkEmpty();
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> {
            if (getParentFragmentManager() != null) {
                getParentFragmentManager().popBackStack();
            }
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

    // This is the checkEmpty() method, now part of the observer's logic.
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

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }
    }
}
