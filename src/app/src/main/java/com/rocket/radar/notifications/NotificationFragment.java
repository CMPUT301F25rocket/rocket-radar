package com.rocket.radar.notifications;

import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {
    // ui components
    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView;
    private Button backButton;

    // Adapter and Data
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    //controller
    NotificationController controller = new NotificationController();

    public NotificationFragment() {
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(getContext(), notificationList);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);
    }

    private void loadDummyData() {
        NotificationController controller = new NotificationController();
        controller.addNotification("Campus Marathon", "Event Update", false, 0, notificationList);
        controller.addNotification("Charity Gala", "Reminder", true, 0, notificationList);
        controller.addNotification("Coding Competition", "Winner Announcement", true, 0, notificationList);
        controller.addNotification("Blood Drive", "New Event", false, 0, notificationList);
        controller.addNotification("Volunteer Meetup", "Feedback Request", true, 0, notificationList);
        adapter.notifyDataSetChanged();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.notification_list, container, false);

        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        backButton = view.findViewById(R.id.back_arrow);

        emptyNotificationsTextView.setVisibility(View.VISIBLE);

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

        notificationList = new ArrayList<>();
        setupRecyclerView();
        loadDummyData();
    }

    private void updateEmptyViewVisibility(){
        emptyNotificationsTextView.setVisibility(View.GONE);
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