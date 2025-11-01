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
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.Event;
import com.rocket.radar.MainActivity;
import com.rocket.radar.R;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {
    // ui components
    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView;
    private Button backButton;
    private com.google.android.material.divider.MaterialDivider divider;

    // Adapter and Data
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    // Repository (Model)
    NotificationRepository notificationRepository;

    public NotificationFragment() {
    }

    private void setupRecyclerView() {
        // Initialize the list first
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(getContext(), notificationList);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);
    }



    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.notification_list, container, false);

        notificationRepository = new NotificationRepository();
        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        backButton = view.findViewById(R.id.back_arrow);
        divider = view.findViewById(R.id.divider);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Initialize UI components
        super.onViewCreated(view, savedInstanceState);

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                // This correctly pops the fragment off the back stack, returning to the previous screen.
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        setupRecyclerView();
        observeEvents();
        // We removed the call to addNotifications() to prevent adding test data every time.
    }

    // This method was for testing and should be removed to show only what's in the DB.
    /*
    private void addNotifications(){
        NotificationRepository repository = new NotificationRepository();
        repository.createNotification(new Notification("BBQ event", "12\nNOV", false, R.drawable.mushroom_in_headphones_amidst_nature));
        repository.createNotification(new Notification("Watch Party for Oilers", "30\nSEP", false, R.drawable.rogers_image));
        repository.createNotification(new Notification("Ski Trip", "18\nDEC", false, R.drawable.ski_trip_banner));
        adapter.notifyDataSetChanged();
    }
    */

    private void observeEvents() {
        // This is the core of the real-time logic.
        // The observer will be called immediately with all existing data in Firestore.
        notificationRepository.getAllNotifications().observe(getViewLifecycleOwner(), new Observer<List<Notification>>() {
            @Override
            public void onChanged(List<Notification> newNotifications) {
                if (newNotifications != null) {
                    Log.d("NotificationListFragment", "Data updated. " + newNotifications.size() + " notification received.");
                    adapter.setNotifications(newNotifications);
                    updateEmptyViewVisibility();
                }
            }
        });
    }

    private void updateEmptyViewVisibility(){
        // Correctly show or hide the empty view and divider
        Log.d("NotificationListFragment", "Updating empty view visibility.");
        if (adapter.getItemCount() == 0) {
            emptyNotificationsTextView.setVisibility(View.VISIBLE);
            divider.setVisibility(View.GONE);
        } else {
            emptyNotificationsTextView.setVisibility(View.GONE);
            divider.setVisibility(View.VISIBLE);
        }
    }

    public void onDestroyView() {
        super.onDestroyView();
        // This is called when the fragment's view is being destroyed, e.g., when popping the back stack.
        if (getActivity() instanceof MainActivity) {
            // Make the bottom navigation view visible again
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
