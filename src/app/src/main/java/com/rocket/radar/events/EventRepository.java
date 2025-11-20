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

/**
 * Event fetching, management and control.
 */
public class EventRepository {

    private static final String TAG = "EventRepository";

    // 🔹 This is now the source of truth for Firestore
    private static FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private final CollectionReference events;
    private static EventRepository instance = null;

    private EventRepository() {
        this.events = firestore.collection("events");
    }

    public static EventRepository getInstance() {
        if (instance == null) {
            instance = new EventRepository();
        }
        return instance;
    }

    // 🔹 Test-only hook: replace Firestore and reset singleton
    public static void useFirestoreForTesting(FirebaseFirestore testFirestore) {
        firestore = testFirestore;
        instance = null; // force re-creation with the new db
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
        events.addSnapshotListener((value, error) -> {
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
        return events.document(eventId).get();
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
        CollectionReference waitlistRef = events.document(event.getEventId())
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
        // Decide which document reference to use
        DocumentReference docRef;

        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            // No ID yet? Let Firestore generate one.
            docRef = events.document();               // auto-ID
            event.setEventId(docRef.getId());         // store it on the Event
        } else {
            // ID already present (e.g., app code set it)
            docRef = events.document(event.getEventId());
        }

        docRef.set(event)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Event successfully written: " + event.getEventTitle()))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error writing event", e));

        // You still return the event ID as before
        return event.getEventId();
    }


    public void addUserToWaitlist(Event event, String userId, GeoPoint location){
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null or has no ID.");
            return;
        }
        else {
            // 1. Get the correct path: events -> {event-id} -> waitlistedUsers -> {user-id}
            DocumentReference waitlistRef = events.document(event.getEventId())
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
        DocumentReference userDocumentInWaitlist = events.document(event.getEventId())
                .collection("waitlistedUsers").document(userId);

        // 2. Call .delete() on that specific document reference.
        userDocumentInWaitlist.delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " successfully removed from waitlist for event " + event.getEventId()))
                .addOnFailureListener(e -> Log.e(TAG, "Error removing user " + userId + " from waitlist", e));
    }

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
        events.document(eventId).collection("waitlistedUsers").document(userId)
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
        events.document(eventId).collection("waitlistedUsers")
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
        events.document(eventId).collection("invitedUsers")
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
        events.document(eventId).collection("invitedUsers").document(userId)
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
        events.document(eventId).collection("invitedUsers")
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
        events.document(eventId).collection("cancelledUsers")
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
        events.document(eventId).collection("cancelledUsers").document(userId)
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
        events.document(eventId).collection("cancelledUsers")
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
        events.document(eventId).collection("selectedUsers")
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
        events.document(eventId).collection("selectedUsers").document(userId)
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
        events.document(eventId).collection("selectedUsers")
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
}
