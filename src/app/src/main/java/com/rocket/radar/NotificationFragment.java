package com.rocket.radar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

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
    //private NotificationAdapter adapter;
    private List<Notification> notificationList;

    public NotificationFragment() {
    }

//    private void setupRecyclerView() {
//        notificationList = new ArrayList<>();
//        NotificationAdapter adapter = new NotificationAdapter(notificationList, this);
//        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//        notificationRecyclerView.setAdapter(adapter);
//    }


    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notification_list, container, false);

        notificationRecyclerView = view.findViewById(R.id.notification_recycler_view);
        emptyNotificationsTextView = view.findViewById(R.id.empty_notifications_text);
        Button backButton = view.findViewById(R.id.back_arrow);

        // setupRecyclerView();

        emptyNotificationsTextView.setVisibility(View.VISIBLE);

        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        return view;
    }






}
