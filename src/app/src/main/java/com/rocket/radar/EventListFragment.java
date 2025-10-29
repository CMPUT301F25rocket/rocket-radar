package com.rocket.radar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends Fragment implements EventAdapter.OnEventListener {

    private RecyclerView eventRecyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;

    public EventListFragment() {
    }

    private void setupRecyclerView() {
        adapter = new EventAdapter(getContext(), eventList, this);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.setAdapter(adapter);
    }

    // Restore your data loading logic here
    private void loadDummyData() {
        eventList.add(new Event(
                "AI Innovation Summit",
                "12\nNOV",
                "Explore the future of artificial intelligence",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));
        eventList.add(new Event(
                "Watch Party for Oilers",
                "18\nDEC",
                "Fun for fanatics",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));

        eventList.add(new Event(
                "Campus Charity Run",
                "03\nDEC",
                "Run for a cause and make a difference",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));

        eventList.add(new Event(
                "Tech Startup Pitch Night",
                "15\nJAN",
                "Where great ideas meet investors",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));

        eventList.add(new Event(
                "Space Exploration Expo",
                "21\nFEB",
                "Discover the latest in rocket and satellite tech",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));

        eventList.add(new Event(
                "Community Blood Drive",
                "10\nMAR",
                "Donate blood, save a life",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));
        // Add more events as needed

        // Notify the adapter that data has been added
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_list, container, false);
        eventRecyclerView = view.findViewById(R.id.event_list_recycler_view);

        // Your other view initializations from event_list.xml
        Button filterButton = view.findViewById(R.id.button_filter);
        Button notificationButton = view.findViewById(R.id.btnNotification);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eventList = new ArrayList<>();
        setupRecyclerView();
        loadDummyData(); // This will now populate your list
    }

    @Override
    public void onEventClick(int position) {
        Event clickedEvent = eventList.get(position);
        EventViewFragment eventViewFragment = EventViewFragment.newInstance(clickedEvent);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, eventViewFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
