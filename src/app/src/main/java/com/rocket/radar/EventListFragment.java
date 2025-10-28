package com.rocket.radar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EventListFragment extends Fragment {

    private List<Event> allEvents;
    private List<Event> waitlistedEvents;
    private EventAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.event_list, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.event_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        allEvents = new ArrayList<>();
        // Add sample data
        List<User> waitlist1 = new ArrayList<>();
        waitlist1.add(new User("Alice"));
        waitlist1.add(new User("Bob"));
        allEvents.add(new Event(System.currentTimeMillis(), "Rocket Launch", R.drawable.ic_notification_24dp, waitlist1, true));

        List<User> waitlist2 = new ArrayList<>();
        waitlist2.add(new User("Charlie"));
        allEvents.add(new Event(System.currentTimeMillis() + 3600000, "Moon Landing", R.drawable.ic_patient_list_fill_24dp, waitlist2, false));

        List<User> waitlist3 = new ArrayList<>();
        waitlist3.add(new User("Dave"));
        allEvents.add(new Event(System.currentTimeMillis() + 7200000, "Mars Rover", R.drawable.ic_search_24dp, waitlist3, true));

        List<User> waitlist4 = new ArrayList<>();
        waitlist4.add(new User("Eve"));
        allEvents.add(new Event(System.currentTimeMillis() + 10800000, "Satellite Deployment", R.drawable.ic_notification_24dp, waitlist4, false));


        waitlistedEvents = allEvents.stream().filter(Event::isOnWaitlist).collect(Collectors.toList());

        adapter = new EventAdapter(allEvents);
        recyclerView.setAdapter(adapter);

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.discover_button) {
                    adapter.setEventList(allEvents);
                } else if (checkedId == R.id.waitlist_button) {
                    adapter.setEventList(waitlistedEvents);
                }
            }
        });

        return view;
    }
}
