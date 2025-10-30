package com.rocket.radar;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rocket.radar.Event;

import java.util.List;

public class EventRepository {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference eventsRef = database.getReference("events");

    private EventAdapter adapter;
    private List<Event> eventList;

    public void createEvent(Event event) {

    }

    // Dummy data:

    private void loadDummyData() {


        eventList.add(new Event(
                "Watch Party for Oilers",
                "30\nSEP",
                "Fun for fanatics",
                R.drawable.rogers_image));
        eventList.add(new Event(
                "BBQ Event",
                "12\nNOV",
                "Mushroom bros who listen to bangers",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));
        eventList.add(new Event(
                "Ski Trip",
                "18\nDEC",
                "The slopes are calling",
                R.drawable.ski_trip_banner));
        eventList.add(new Event(
                "Ski Trip",
                "18\nDEC",
                "The slopes are calling",
                R.drawable.ski_trip_banner));
        eventList.add(new Event(
                "Ski Trip",
                "18\nDEC",
                "The slopes are calling",
                R.drawable.ski_trip_banner));

        eventList.add(new Event(
                "Penultimate event",
                "21\nFEB",
                "Second to last item in list",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));

        eventList.add(new Event(
                "Last event",
                "10\nMAR",
                "Last item in list",
                com.rocket.radar.R.drawable.mushroom_in_headphones_amidst_nature));
    }

    private void addDummyDatatodb(List<Event> eventList) {
        for (Event event; eventList) {
            createEvent(event);
        }
    }





}
