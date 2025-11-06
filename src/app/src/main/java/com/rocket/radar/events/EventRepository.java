package com.rocket.radar.events;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rocket.radar.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
                    Event event = doc.toObject(Event.class);
                    eventList.add(event);
                }
            }
            eventsLiveData.postValue(eventList);
        });
        return eventsLiveData;
    }

    public interface WaitlistSizeListener {
        void onSizeReceived(int size);
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the size of the waitlist for a given event.
     * @param event The event whose waitlist size is needed.
     * @param listener The callback to be invoked with the result.
     */
    public void getWaitlistSize(Event event, WaitlistSizeListener listener) {
        if (event == null || event.getEventTitle() == null || event.getEventTitle().isEmpty()) {
            Log.e(TAG, "Event is null or has no Title.");
            listener.onError(new IllegalArgumentException("Event is null or has no title"));
            return;
        }

        // CORRECT PATH: events -> {event-id} -> waitlistedUsers
        // IMPORTANT: I noticed you are using event.getEventTitle() as the document ID. This is risky if titles can change or are not unique.
        // It's better to use event.getEventId(). For now, I'll stick to your current implementation.
        CollectionReference waitlistRef = db.collection("events").document(event.getEventTitle())
                .collection("waitlistedUsers");

        waitlistRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            // This code runs when the database call is successful.
            listener.onSizeReceived(queryDocumentSnapshots.size());
        }).addOnFailureListener(e -> {
            // This code runs if the call fails.
            Log.e(TAG, "Error getting waitlist size", e);
            listener.onError(e);
        });
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

    public void addUserToWaitlist(Event event, String userId, GeoPoint location){
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null or has no ID.");
            return;
        }

        // Use a more descriptive name for the sub-collection, like "checkins".
        DocumentReference checkinRef = db.collection("events").document(event.getEventTitle())
                .collection("waitlistedUsers").document(userId);

        Map<String, Object> checkinData = new HashMap<>();
        checkinData.put("userId", userId);
        checkinData.put("signupTimestamp", FieldValue.serverTimestamp());
        // Only add the location if it's not null
        if (location != null) {
            checkinData.put("signupLocation", location);
        } else {
            Log.w(TAG, "User location is null. Not adding to check-in document.");
        }

        checkinRef.set(checkinData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " check-in for event " + event.getEventId() + " created."))
                .addOnFailureListener(e -> Log.e(TAG, "Error creating check-in for user " + userId, e));
    }

    public void removeUserFromWaitlist(Event event, String userId) {
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null or has no ID. Cannot remove user from waitlist.");
            return;
        }
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty. Cannot remove user from waitlist.");
            return;
        }
        DocumentReference userDocumentInWaitlist = db.collection("events").document(event.getEventTitle())
                .collection("waitlistedUsers").document(userId);

        // 2. Call .delete() on that specific document reference.
        userDocumentInWaitlist.delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " successfully removed from waitlist for event " + event.getEventId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error removing user " + userId + " from waitlist", e));
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

    // --- START OF FIX: ADD THE MISSING INTERFACE AND METHOD ---

    /**
     * Callback interface for fetching waitlist entrants.
     */
    public interface WaitlistEntrantsCallback {
        /**
         * Called when the list of user names (entrants) is successfully fetched.
         * @param userNames A list of user IDs from the waitlist.
         */
        void onWaitlistEntrantsFetched(List<String> userNames);

        /**
         * Called when an error occurs while fetching the waitlist.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of user IDs from the waitlist of a specific event.
     *
     * @param eventId The ID of the event to fetch the waitlist for.
     * @param callback The callback to handle the success or failure of the operation.
     */
    public void getWaitlistEntrants(String eventId, WaitlistEntrantsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }

        // The path is events -> {eventId} -> waitlistedUsers
        db.collection("events").document(eventId).collection("waitlistedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();
                    // The document ID of each document in the 'waitlistedUsers' subcollection is the user's ID.

                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
                    callback.onWaitlistEntrantsFetched(userIds);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- START OF NEW METHODS ---

    /**
     * Callback interface for fetching a user's location from the waitlist.
     */
    public interface UserLocationCallback {
        void onLocationFetched(GeoPoint location);
        void onError(Exception e);
    }

    /**
     * Fetches the GeoPoint for a specific user from an event's waitlist.
     *
     * @param eventId  The ID of the event.
     * @param userId   The ID of the user whose location is to be fetched.
     * @param callback The callback to handle the result.
     */
    public void getUserLocationFromWaitlist(String userId, String eventId, UserLocationCallback callback) {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID and User ID cannot be null or empty."));
            return;
        }

        // The path is events -> {eventId} -> waitlistedUsers -> {userId}
        db.collection("events").document(eventId).collection("waitlistedUsers").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        GeoPoint location = documentSnapshot.getGeoPoint("signupLocation");
                        callback.onLocationFetched(location); // Can be null if field doesn't exist
                    } else {
                        callback.onError(new Exception("User " + userId + " not found in waitlist for event " + eventId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }


    /**
     * Callback interface for fetching waitlist locations.
     */
    public interface WaitlistLocationsCallback {
        /**
         * Called when the list of GeoPoint locations is successfully fetched.
         * @param locations A list of GeoPoint objects from the waitlist.
         */
        void onWaitlistLocationsFetched(List<GeoPoint> locations);

        /**
         * Called when an error occurs while fetching the waitlist locations.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of signup locations from the waitlist of a specific event.
     *
     * @param eventId The ID of the event to fetch the waitlist locations for.
     * @param callback The callback to handle the success or failure of the operation.
     */
    public void getWaitlistLocations(String eventId, WaitlistLocationsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }

        // The path is events -> {eventId} -> waitlistedUsers
        db.collection("events").document(eventId).collection("waitlistedUsers")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<GeoPoint> locations = new ArrayList<>();
                // Iterate through each document in the 'waitlistedUsers' subcollection.
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    // Try to get the 'signupLocation' field which is a GeoPoint.
                    GeoPoint location = doc.getGeoPoint("signupLocation");
                    if (location != null) {
                        locations.add(location);
                    } else {
                        Log.w(TAG, "Document " + doc.getId() + " in waitlist for event " + eventId + " does not have a signupLocation.");
                    }
                }
                callback.onWaitlistLocationsFetched(locations);
            })
            .addOnFailureListener(callback::onError);
    }

    // --- END OF NEW METHODS ---
    // --- END OF FIX ---
}
