// C:/Users/bwood/Cmput301/rocket-radar/src/app/src/main/java/com/rocket/radar/EventListFragment.java
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

    // ================== TESTING FLAG ==================
    // Set this to 'true' to test the organizer view, 'false' for normal user view.
    private static final boolean TESTING_ORGANIZER_VIEW = true;
    // ================================================

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
            NotificationFragment notificationFragment = new NotificationFragment();

            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);

                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, notificationFragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        // Start observing the data from the repository
        observeEvents();

        // Optional: Comment this out after the first run.
        // eventRepository.addDummyDatatodb();
    }

    private void observeEvents() {
        eventRepository.getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            if (newEvents != null) {
                Log.d("EventListFragment", "Data updated. " + newEvents.size() + " events received.");
                eventList.clear();
                eventList.addAll(newEvents);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onEventClick(int position) {
        // Hide the bottom navigation bar before navigating to any detail screen
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.GONE);
        }

        if (TESTING_ORGANIZER_VIEW) {
            // --- TEMPORARY TESTING LOGIC ---
            Log.d("EventListFragment", "TESTING_ORGANIZER_VIEW is true. Navigating to OrganizerEntrantsFragment.");
            OrganizerEntrantsFragment organizerFragment = new OrganizerEntrantsFragment();

            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, organizerFragment)
                        .addToBackStack(null)
                        .commit();
            }

        } else {
            // --- NORMAL USER LOGIC ---
            Log.d("EventListFragment", "TESTING_ORGANIZER_VIEW is false. Navigating to EventViewFragment.");
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

    @Override
    public void onResume() {
        super.onResume();
        // Make sure the bottom navigation is visible when the user returns to this screen
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
    }
}
