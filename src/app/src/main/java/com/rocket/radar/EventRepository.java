package com.rocket.radar;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rocket.radar.Event;

import java.util.Collections;
import java.util.List;

public class EventRepository {

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference eventsRef = database.getReference("events");




    public void createEvent(Event event) {
        eventsRef.push().setValue(event);
    }

    // Dummy data:

    private List<Event> loadDummyData() {
        List<Event> eventList = Collections.emptyList();


        eventList.add(new Event(
                "Watch Party for Oilers",
                "30\nSEP",
                "Fun for fanatics",
                R.drawable.rogers_image));
        eventList.add(new Event(
                "BBQ Event",
                "12\nNOV",
                "Mushroom bros who listen to bangers",
                R.drawable.mushroom_in_headphones_amidst_nature));
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
                R.drawable.mushroom_in_headphones_amidst_nature));

        eventList.add(new Event(
                "Last event",
                "10\nMAR",
                "Last item in list",
                R.drawable.mushroom_in_headphones_amidst_nature));

        return eventList;
    }

    private void addDummyDatatodb(List<Event> eventList) {
        for (Event event = null; eventList) {
            createEvent(event);
        }
    }





}
