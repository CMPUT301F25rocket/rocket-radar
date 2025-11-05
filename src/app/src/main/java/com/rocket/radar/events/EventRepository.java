package com.rocket.radar.events;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rocket.radar.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Date;

public class EventRepository {

    private static final String TAG = "EventRepository";
    private final FirebaseFirestore db;
    private final CollectionReference eventRef;

    // This constructor now correctly initializes Firestore.
    public EventRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.eventRef = db.collection("events"); // Use "events" collection
    }

    /**
     * This is the method you asked for, adapted from your lab.
     * It listens for real-time updates from the "events" collection in Firestore
     * and returns the data wrapped in LiveData.
     */
    public LiveData<List<Event>> getAllEvents() {
        MutableLiveData<List<Event>> eventsLiveData = new MutableLiveData<>();

        eventRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }

            ArrayList<Event> eventList = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    // Convert each document into an Event object
                    Event event = doc.toObject(Event.class);
                    eventList.add(event);
                }
            }
            // Post the new list to observers (like your fragment)
            eventsLiveData.postValue(eventList);
        });

        return eventsLiveData;
    }

    // This method adds a new event to Firestore.
    public void createEvent(Event event) {
        // Use the event's title as the document ID for simplicity, or use .add() for auto-ID
        eventRef.document(event.getEventTitle()).set(event)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Event successfully written: " + event.getEventTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Error writing event", e));
    }

    // Helper to add all the dummy data to Firestore
    public void addDummyDatatodb() {
        List<Event> dummyEvents = loadDummyData();
        for (Event event : dummyEvents) {
            createEvent(event);
        }
    }

    // This method just prepares the local list of dummy data.
    private List<Event> loadDummyData() {
        List<Event> eventList = new java.util.ArrayList<>();

        // Using Calendar to create Date objects for the current year
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        cal.set(currentYear, Calendar.SEPTEMBER, 30);
        eventList.add(new Event("Watch Party for Oilers", cal.getTime(), "Fun for fanatics", R.drawable.rogers_image));
        cal.set(currentYear, Calendar.NOVEMBER, 12);
        eventList.add(new Event("BBQ Event", cal.getTime(), "Mushroom bros who listen to bangers", R.drawable.mushroom_in_headphones_amidst_nature));
        cal.set(currentYear, Calendar.DECEMBER, 18);
        eventList.add(new Event("Ski Trip", cal.getTime(), "The slopes are calling", R.drawable.ski_trip_banner));
        cal.set(currentYear + 1, Calendar.JANUARY, 5); // Next year for January
        eventList.add(new Event("Tech Conference", cal.getTime(), "Innovations in AI", R.drawable.rogers_image));
        cal.set(currentYear, Calendar.JULY, 22);
        eventList.add(new Event("Summer Music Festival", cal.getTime(), "Live bands and good vibes", R.drawable.mushroom_in_headphones_amidst_nature));
        cal.set(currentYear, Calendar.AUGUST, 14);
        eventList.add(new Event("Mountain Hike", cal.getTime(), "Explore scenic trails", R.drawable.ski_trip_banner));

        return eventList;
    }
}
