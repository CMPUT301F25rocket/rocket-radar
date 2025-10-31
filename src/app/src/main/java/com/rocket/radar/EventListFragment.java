package com.rocket.radar;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rocket.radar.notifications.NotificationFragment;

import java.util.ArrayList;
import java.util.List;

public class EventListFragment extends Fragment implements EventAdapter.OnEventListener {

    private RecyclerView eventRecyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;
    private EventRepository eventRepository;
    private Button notificationButton;


    public EventListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_list, container, false);
        eventRecyclerView = view.findViewById(R.id.event_list_recycler_view);
        notificationButton = view.findViewById(R.id.btnNotification);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialization
        eventRepository = new EventRepository();
        eventList = new ArrayList<>();
        adapter = new EventAdapter(getContext(), eventList, this);
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventRecyclerView.setAdapter(adapter);

        notificationButton.setOnClickListener(v -> {
            // Create the NotificationFragment using its empty constructor.
            // It does not need any arguments.
            NotificationFragment notificationFragment = new NotificationFragment();

            if (getActivity() instanceof MainActivity) { // Check if the activity is MainActivity
                // Hide the bottom navigation view
                ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);

                getActivity().getSupportFragmentManager().beginTransaction()
                        // Use your actual FragmentContainerView ID
                        .replace(R.id.nav_host_fragment, notificationFragment)
                        .addToBackStack(null) // Allows the user to return with the back button
                        .commit();
            }
        });

        // Start observing the data from the repository
        observeEvents();

        // Optional: Call this once if you want to ensure dummy data exists in
        // Firestore.
        // You can comment this out after the first run.
        eventRepository.addDummyDatatodb();
    }

    private void observeEvents() {
        // This is the core of the real-time logic.
        // The observer will be called every time data changes in Firestore.
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), new Observer<List<Event>>() {
            @Override
            public void onChanged(List<Event> newEvents) {
                if (newEvents != null) {
                    Log.d("EventListFragment", "Data updated. " + newEvents.size() + " events received.");
                    eventList.clear();
                    eventList.addAll(newEvents);
                    adapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onEventClick(int position) {
        Event clickedEvent = eventList.get(position);
        EventViewFragment eventViewFragment = EventViewFragment.newInstance(clickedEvent);

        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, eventViewFragment)
                    .addToBackStack(null)
                    .commit();
        }
    }
}
