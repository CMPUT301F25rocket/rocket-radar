package com.rocket.radar;

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

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {
    // ui components
    private RecyclerView notificationRecyclerView;
    private TextView emptyNotificationsTextView;

    // Adapter and Data
    private NotificationAdapter adapter;
    private List<Notification> notificationList;

    public NotificationFragment() {
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter(getContext(), notificationList);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationRecyclerView.setAdapter(adapter);
    }

    private void loadDummyData() {
        Notification dummyNotif1 = new Notification("Campus Marathon", "Event Update", false, 0);
        Notification dummyNotif2 = new Notification("Charity Gala", "Reminder", true, 0);
        Notification dummyNotif3 = new Notification("Coding Competition", "Winner Announcement", false, 0);
        Notification dummyNotif4 = new Notification("Blood Drive", "New Event", false, 0);
        Notification dummyNotif5 = new Notification("Volunteer Meetup", "Feedback Request", true, 0);
        notificationList.add(dummyNotif1);
        notificationList.add(dummyNotif2);
        notificationList.add(dummyNotif3);
        notificationList.add(dummyNotif4);
        notificationList.add(dummyNotif5);
        adapter.notifyDataSetChanged();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.notification_list, container, false);

        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        Button backButton = view.findViewById(R.id.back_arrow);

        emptyNotificationsTextView.setVisibility(View.VISIBLE);

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Initialize UI components
        super.onViewCreated(view, savedInstanceState);

        notificationList = new ArrayList<>();
        setupRecyclerView();
        loadDummyData();
    }

    private void updateEmptyViewVisibility(){
        emptyNotificationsTextView.setVisibility(View.GONE);
    }
}