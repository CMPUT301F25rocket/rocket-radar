package com.rocket.radar.events;

import android.graphics.Bitmap;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rocket.radar.R;

import org.checkerframework.common.returnsreceiver.qual.This;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.util.Map;

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

    /**
     * This method adds a new event to Firestore.
     * @return The UUID of the created event.
     */
    public String createEvent(Event event) {
        // Use the event's title as the document ID for simplicity, or use .add() for auto-ID
        eventRef.document(event.getEventId()).set(event)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Event successfully written: " + event.getEventTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Error writing event", e));
        return event.getEventId();
    }

    // Helper to add all the dummy data to Firestore
    public void addDummyDatatodb() {
        List<Event> dummyEvents = loadDummyData();
        for (Event event : dummyEvents) {
            createEvent(event);
        }
    }

    public void addUserToWaitlist(Event event, String userId){
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null or has no ID.");
            return;
        }
        else {
            // --- START OF FIX ---
            // 1. Get the correct path: events -> {event-id} -> waitlistedUsers -> {user-id}
            DocumentReference waitlistRef = db.collection("events").document(event.getEventTitle())
                    .collection("waitlistedUsers").document(userId);

            // 2. Create a map to hold some data, like a timestamp.
            // Firestore documents cannot be completely empty.
            Map<String, Object> waitlistData = new HashMap<>();
            waitlistData.put("timestamp", FieldValue.serverTimestamp());

            // 3. Set the data. If the document already exists, this overwrites it but
            // that's fine. If it doesn't exist, it is created.
            waitlistRef.set(waitlistData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " successfully added to waitlist for event " + event.getEventId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding user to waitlist", e));
            // --- END OF FIX ---
        }
    }

    // This method just prepares the local list of dummy data.
    private List<Event> loadDummyData() {
        List<Event> eventList = new java.util.ArrayList<>();

        // Using Calendar to create Date objects for the current year
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        cal.set(currentYear, Calendar.SEPTEMBER, 30);
        eventList.add(new Event("Watch Party for Oilers", cal.getTime(), "Fun for fanatics", "Join us for an exciting watch party as the Oilers take on their rivals. Great food, great company, and a thrilling game await. Don't miss out on the action!", R.drawable.rogers_image));

        cal.set(currentYear, Calendar.NOVEMBER, 12);
        eventList.add(new Event("BBQ Event", cal.getTime(), "Mushroom bros who listen to bangers", "A chill BBQ event for everyone who enjoys good music and even better food. We'll be grilling up a storm and spinning some bangers. Come hang out!", R.drawable.mushroom_in_headphones_amidst_nature));
        cal.set(currentYear, Calendar.DECEMBER, 18);
        eventList.add(new Event("Ski Trip", cal.getTime(), "The slopes are calling", "Hit the slopes with us for a weekend of skiing and snowboarding. All skill levels are welcome. Get ready for some fresh powder and stunning mountain views.", R.drawable.ski_trip_banner));
        cal.set(currentYear + 1, Calendar.JANUARY, 5); // Next year for January
        eventList.add(new Event("Tech Conference", cal.getTime(), "Innovations in AI", "Discover the latest breakthroughs in Artificial Intelligence at our annual Tech Conference. Featuring keynote speakers from leading tech companies and interactive workshops.", R.drawable.rogers_image));
        cal.set(currentYear, Calendar.JULY, 22);
        eventList.add(new Event("Summer Music Festival", cal.getTime(), "Live bands and good vibes", "Experience the best of summer with our annual music festival. Featuring a lineup of incredible live bands, food trucks, and a vibrant atmosphere. Let the good times roll!", R.drawable.mushroom_in_headphones_amidst_nature));
        cal.set(currentYear, Calendar.AUGUST, 14);
        eventList.add(new Event("Mountain Hike", cal.getTime(), "Explore scenic trails", "Join our guided hike through breathtaking mountain trails. This is a great opportunity to connect with nature, get some exercise, and enjoy panoramic views.", R.drawable.ski_trip_banner));

        return eventList;
    }
}
