package com.rocket.radar.events;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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

// FIXME: This is functionally a singleton we should store the global instance in a static and return
// that instead of creating many of these objects.
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
    // FIXME: This is bad practice and going to spike our firestore reads really hard.
    // TODO: EventListFragment should query the firestore for events upcoming soon, and as the user
    // nears the bottom of the list should load additional events as they are required.
    // But that's hard and annoying so part 4 it is.
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

    /**
     * @param eventId UUID of the event we want to fetch.
     * @return Task yielding a {@code DocumentSnapshot} which can be converted into an {@code Event}
     */
    public Task<DocumentSnapshot> getEvent(String eventId) {
        return eventRef.document(eventId).get();
    }

    public interface WaitlistSizeListener {
        void onSizeReceived(int size);

        void onWaitlistEntrantsFetched(List<String> userIds);
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
            List<String> userIds = new ArrayList<>();
            queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
            listener.onWaitlistEntrantsFetched(userIds);
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
        else {
            // --- START OF FIX ---
            // 1. Get the correct path: events -> {event-id} -> waitlistedUsers -> {user-id}
            DocumentReference waitlistRef = db.collection("events").document(event.getEventId())
                    .collection("waitlistedUsers").document(userId);

            // 2. Create a map to hold some data, like a timestamp.
            // Firestore documents cannot be completely empty.

            Map<String, Object> waitlistData = new HashMap<>();
            waitlistData.put("timestamp", FieldValue.serverTimestamp());
            if (location != null) {
                waitlistData.put("signupLocation", location);
            } else {
                Log.w(TAG, "User location is null. Not adding to check-in document.");
            }

            // 3. Set the data. If the document already exists, this overwrites it but
            // that's fine. If it doesn't exist, it is created.
            waitlistRef.set(waitlistData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " successfully added to waitlist for event " + event.getEventId()))
                    .addOnFailureListener(e -> Log.e(TAG, "Error adding user to waitlist", e));
            // --- END OF FIX ---
        }
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
         * @param userIds A list of user IDs from the waitlist.
         */
        void onWaitlistEntrantsFetched(List<String> userIds);

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
//    public void getWaitlistEntrants(Event event, WaitlistEntrantsCallback callback) {
//        if (event.getEventTitle() == null) {
//            Log.e(TAG, "Event is null or has no title.");
//            return;
//        }
//
//        // The path is events -> {eventId} -> waitlistedUsers
//        db.collection("events").document(event.getEventTitle()).collection("waitlistedUsers")
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    List<String> userIds = new ArrayList<>();
//                    // The document ID of each document in the 'waitlistedUsers' subcollection is the user's ID.
//
//                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
//                    callback.onWaitlistEntrantsFetched(userIds);
//                })
//                .addOnFailureListener(callback::onError);
//    }

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

    // --- START OF INVITED ENTRANTS METHODS ---

    /**
     * Callback interface for fetching invited entrants.
     */
    public interface InvitedEntrantsCallback {
        void onInvitedEntrantsFetched(List<String> userIds);
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of user IDs from the invited list of a specific event.
     */
    public void getInvitedEntrants(String eventId, InvitedEntrantsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("invitedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
                    callback.onInvitedEntrantsFetched(userIds);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for fetching a user's location from the invited list.
     */
    public interface UserLocationFromInvitedCallback {
        void onLocationFetched(GeoPoint location);
        void onError(Exception e);
    }

    /**
     * Fetches the GeoPoint for a specific user from an event's invited list.
     */
    public void getUserLocationFromInvited(String userId, String eventId, UserLocationFromInvitedCallback callback) {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID and User ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("invitedUsers").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        GeoPoint location = documentSnapshot.getGeoPoint("signupLocation");
                        callback.onLocationFetched(location);
                    } else {
                        callback.onError(new Exception("User " + userId + " not found in invited list for event " + eventId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for fetching invited list locations.
     */
    public interface InvitedLocationsCallback {
        void onInvitedLocationsFetched(List<GeoPoint> locations);
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of signup locations from the invited list of a specific event.
     */
    public void getInvitedLocations(String eventId, InvitedLocationsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("invitedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<GeoPoint> locations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GeoPoint location = doc.getGeoPoint("signupLocation");
                        if (location != null) {
                            locations.add(location);
                        } else {
                            Log.w(TAG, "Document " + doc.getId() + " in invited list for event " + eventId + " does not have a signupLocation.");
                        }
                    }
                    callback.onInvitedLocationsFetched(locations);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- END OF INVITED ENTRANTS METHODS ---

    // --- START OF CANCELLED ENTRANTS METHODS ---

    /**
     * Callback interface for fetching cancelled entrants.
     */
    public interface CancelledEntrantsCallback {
        void onCancelledEntrantsFetched(List<String> userIds);
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of user IDs from the cancelled list of a specific event.
     */
    public void getCancelledEntrants(String eventId, CancelledEntrantsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("cancelledUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
                    callback.onCancelledEntrantsFetched(userIds);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for fetching a user's location from the cancelled list.
     */
    public interface UserLocationFromCancelledCallback {
        void onLocationFetched(GeoPoint location);
        void onError(Exception e);
    }

    /**
     * Fetches the GeoPoint for a specific user from an event's cancelled list.
     */
    public void getUserLocationFromCancelled(String userId, String eventId, UserLocationFromCancelledCallback callback) {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID and User ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("cancelledUsers").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        GeoPoint location = documentSnapshot.getGeoPoint("signupLocation");
                        callback.onLocationFetched(location);
                    } else {
                        callback.onError(new Exception("User " + userId + " not found in cancelled list for event " + eventId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for fetching cancelled list locations.
     */
    public interface CancelledLocationsCallback {
        void onCancelledLocationsFetched(List<GeoPoint> locations);
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of signup locations from the cancelled list of a specific event.
     */
    public void getCancelledLocations(String eventId, CancelledLocationsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("cancelledUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<GeoPoint> locations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GeoPoint location = doc.getGeoPoint("signupLocation");
                        if (location != null) {
                            locations.add(location);
                        } else {
                            Log.w(TAG, "Document " + doc.getId() + " in cancelled list for event " + eventId + " does not have a signupLocation.");
                        }
                    }
                    callback.onCancelledLocationsFetched(locations);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- END OF CANCELLED ENTRANTS METHODS ---

    // --- START OF SELECTED ENTRANTS METHODS ---

    /**
     * Callback interface for fetching selected entrants.
     */
    public interface SelectedEntrantsCallback {
        void onSelectedEntrantsFetched(List<String> userIds);
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of user IDs from the selected list of a specific event.
     */
    public void getSelectedEntrants(String eventId, SelectedEntrantsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("selectedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
                    callback.onSelectedEntrantsFetched(userIds);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for fetching a user's location from the selected list.
     */
    public interface UserLocationFromSelectedCallback {
        void onLocationFetched(GeoPoint location);
        void onError(Exception e);
    }

    /**
     * Fetches the GeoPoint for a specific user from an event's selected list.
     */
    public void getUserLocationFromSelected(String userId, String eventId, UserLocationFromSelectedCallback callback) {
        if (eventId == null || eventId.isEmpty() || userId == null || userId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID and User ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("selectedUsers").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        GeoPoint location = documentSnapshot.getGeoPoint("signupLocation");
                        callback.onLocationFetched(location);
                    } else {
                        callback.onError(new Exception("User " + userId + " not found in selected list for event " + eventId));
                    }
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for fetching selected list locations.
     */
    public interface SelectedLocationsCallback {
        void onSelectedLocationsFetched(List<GeoPoint> locations);
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of signup locations from the selected list of a specific event.
     */
    public void getSelectedLocations(String eventId, SelectedLocationsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }
        db.collection("events").document(eventId).collection("selectedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<GeoPoint> locations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        GeoPoint location = doc.getGeoPoint("signupLocation");
                        if (location != null) {
                            locations.add(location);
                        } else {
                            Log.w(TAG, "Document " + doc.getId() + " in selected list for event " + eventId + " does not have a signupLocation.");
                        }
                    }
                    callback.onSelectedLocationsFetched(locations);
                })
                .addOnFailureListener(callback::onError);
    }

    // --- END OF SELECTED ENTRANTS METHODS ---

    // --- END OF NEW METHODS ---
    // --- END OF FIX ---

}
