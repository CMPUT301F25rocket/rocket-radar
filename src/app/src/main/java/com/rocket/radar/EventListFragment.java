package com.rocket.radar;

import android.app.Notification;
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

import com.rocket.radar.databinding.EventListBinding;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends Fragment {

    private EventListBinding binding;

    // ui components
    private RecyclerView eventRecyclerView;

    // Adapter and Data
    private EventAdapter adapter;
    private List<Event> eventList;


    public EventListFragment() {
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(getContext(), eventList);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.setAdapter(adapter);
    }

    private void loadDummyData() {
        //ArrayList<Event> eventList = new ArrayList<>();

        eventList.add(new Event(
                "AI Innovation Summit",
                "12\nNOV",
                "Explore the future of artificial intelligence",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature
        ));
        eventList.add(new Event(
                "Watch Party for Oilers",
                "18\nDEC",
                "Fun for fanatics",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature
                ));

        eventList.add(new Event(
                "Campus Charity Run",
                "03\nDEC",
                "Run for a cause and make a difference",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature
        ));

        eventList.add(new Event(
                "Tech Startup Pitch Night",
                "15\nJAN",
                "Where great ideas meet investors",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature
        ));

        eventList.add(new Event(
                "Space Exploration Expo",
                "21\nFEB",
                "Discover the latest in rocket and satellite tech",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature
        ));

        eventList.add(new Event(
                "Community Blood Drive",
                "10\nMAR",
                "Donate blood, save a life",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature
        ));

        adapter.notifyDataSetChanged();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.event_list, container, false);

        eventRecyclerView = view.findViewById(R.id.event_list_recycler_view);
        Button notificationButton = view.findViewById(R.id.btnNotification);


        notificationButton.setOnClickListener(v -> {
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

        eventList = new ArrayList<>();
        setupRecyclerView();
        loadDummyData();
    }



//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        binding = EventListBinding.inflate(inflater, container, false);
//        return binding.getRoot();
//    }
}
