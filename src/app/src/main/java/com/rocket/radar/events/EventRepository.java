package com.rocket.radar.events;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event fetching, management and control.
 */
public class EventRepository {

    private static final String TAG = "EventRepository";

    // Source of truth for Firestore
    private static FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private final CollectionReference events;
    private static EventRepository instance = null;

    public EventRepository() {
        this.events = firestore.collection("events");
    }

    /**
     * Gets the singleton instance of EventRepository.
     * @return The EventRepository instance.
     */
    public static EventRepository getInstance() {
        if (instance == null) {
            instance = new EventRepository();
        }
        return instance;
    }

    /**
     * Test-only hook to replace Firestore instance and reset singleton.
     * This should only be used in unit tests.
     * @param testFirestore The test Firestore instance to use.
     */
    public static void useFirestoreForTesting(FirebaseFirestore testFirestore) {
        firestore = testFirestore;
        instance = null;
    }

    /**
     * Listens for real-time updates from the "events" collection.
     * Because this uses addSnapshotListener, new events created will
     * automatically trigger this and update the LiveData.
     */
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
     * Gets an event document from Firestore by its ID.
     * @param eventId The ID of the event to retrieve.
     * @return A Task containing the DocumentSnapshot for the event.
     */
    public Task<DocumentSnapshot> getEvent(String eventId) {
        return events.document(eventId).get();
    }

    /**
     * Asynchronously fetches an event by its ID and returns it via callback.
     * @param eventId The ID of the event to retrieve.
     * @param listener Callback to handle success or failure.
     */
    public void getEventById(String eventId, SingleEventListener listener) {
        events.document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Event event = documentSnapshot.toObject(Event.class);
                    listener.onEventLoaded(event);
                })
                .addOnFailureListener(listener::onError);
    }

    /**
     * Callback interface for fetching a single event.
     */
    public interface SingleEventListener {
        /**
         * Called when the event is successfully loaded.
         * @param event The loaded Event object.
         */
        void onEventLoaded(Event event);

        /**
         * Called when an error occurs while loading the event.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Adds a user to the attending list of an event.
     * @param event The event to add the user to.
     * @param uid The user ID to add.
     */
    public void addUserToAttending(Event event, String uid) {
        if (event == null || event.getEventId() == null) return;

        DocumentReference attendingRef = events.document(event.getEventId())
                .collection("attendingUsers").document(uid);

        Map<String, Object> attendingData = new HashMap<>();
        attendingData.put("timestamp", FieldValue.serverTimestamp());

        attendingRef.set(attendingData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User added to attending: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding to attending", e));
    }

    /**
     * Adds a user to the cancelled list of an event.
     * @param event The event to add the user to.
     * @param uid The user ID to add.
     */
    public void addUserToCancelled(Event event, String uid) {
        if (event == null || event.getEventId() == null) return;

        DocumentReference cancelledRef = events.document(event.getEventId())
                .collection("cancelledUsers").document(uid);

        Map<String, Object> cancelledData = new HashMap<>();
        cancelledData.put("timestamp", FieldValue.serverTimestamp());

        cancelledRef.set(cancelledData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User added to cancelled: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding to cancelled", e));
    }

    /**
     * Deletes the banner image from an event in Firestore.
     * @param event The event whose image should be deleted.
     * @param successListener Callback for successful deletion.
     * @param failureListener Callback for failed deletion.
     */
    public void deleteImage(Event event, OnSuccessListener<? super Void> successListener, OnFailureListener failureListener) {
        events.document(event.getEventId()).update("bannerImageBlob", FieldValue.delete())
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    /**
     * Removes a user from the invited list of an event.
     * @param event The event to remove the user from.
     * @param uid The user ID to remove.
     */
    public void removeUserFromInvited(Event event, String uid) {
        if (event == null || event.getEventId() == null || uid == null) return;

        events.document(event.getEventId())
                .collection("invitedUsers").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User removed from invited: " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "Error removing from invited", e));
    }

    // --- Waitlist Size Logic ---
    /**
     * Callback interface for fetching waitlist size and entrants.
     */
    public interface WaitlistSizeListener {
        /**
         * Called when the waitlist size is successfully fetched.
         * @param size The number of users on the waitlist.
         */
        void onSizeReceived(int size);

        /**
         * Called when the list of waitlist user IDs is fetched.
         * @param userIds The list of user IDs on the waitlist.
         */
        void onWaitlistEntrantsFetched(List<String> userIds);

        /**
         * Called when an error occurs.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Fetches the size and list of users on the waitlist for an event.
     * @param event The event to fetch waitlist information for.
     * @param listener Callback to handle the result.
     */
    public void getWaitlistSize(Event event, WaitlistSizeListener listener) {
        if (event == null || event.getEventId() == null) {
            listener.onError(new IllegalArgumentException("Event is null or has no ID"));
            return;
        }

        events.document(event.getEventId()).collection("waitlistedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onSizeReceived(queryDocumentSnapshots.size());
                    List<String> userIds = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
                    listener.onWaitlistEntrantsFetched(userIds);
                })
                .addOnFailureListener(listener::onError);
    }

    // --- Invited Size Logic ---
    /**
     * Callback interface for fetching invited size and entrants.
     */
    public interface InvitedSizeListener {
        /**
         * Called when the invited list size is successfully fetched.
         * @param size The number of users invited.
         */
        void onSizeReceived(int size);

        /**
         * Called when the list of invited user IDs is fetched.
         * @param userIds The list of user IDs invited.
         */
        void onInvitedEntrantsFetched(List<String> userIds);

        /**
         * Called when an error occurs.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Fetches the size and list of users invited to an event.
     * @param event The event to fetch invited information for.
     * @param listener Callback to handle the result.
     */
    public void getInvitedSize(Event event, InvitedSizeListener listener) {
        if (event == null || event.getEventId() == null) {
            listener.onError(new IllegalArgumentException("Event is null or has no ID"));
            return;
        }

        events.document(event.getEventId()).collection("invitedUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listener.onSizeReceived(queryDocumentSnapshots.size());
                    List<String> userIds = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
                    listener.onInvitedEntrantsFetched(userIds);
                })
                .addOnFailureListener(listener::onError);
    }

    // --- Cancelled Size Logic ---
    /**
     * Callback interface for fetching cancelled list size.
     */
    public interface CancelledSizeListener {
        /**
         * Called when the cancelled list size is successfully fetched.
         * @param size The number of users who cancelled.
         */
        void onSizeReceived(int size);

        /**
         * Called when an error occurs.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Fetches the size of the cancelled list for an event.
     * @param event The event to fetch cancelled information for.
     * @param listener Callback to handle the result.
     */
    public void getCancelledSize(Event event, CancelledSizeListener listener) {
        if (event == null || event.getEventId() == null) {
            listener.onError(new IllegalArgumentException("Event is null or has no ID"));
            return;
        }

        events.document(event.getEventId()).collection("cancelledUsers")
                .get()
                .addOnSuccessListener(q -> listener.onSizeReceived(q.size()))
                .addOnFailureListener(listener::onError);
    }

    /**
     * Creates a new event in Firestore or updates an existing one.
     * If the event doesn't have an ID, one is generated. If the event doesn't have an organizer ID,
     * the current user is automatically assigned as the organizer.
     * @param event The event to create or update.
     * @return The event ID.
     */
    public String createEvent(Event event) {
        DocumentReference docRef;

        if (event.getEventId() == null || event.getEventId().isEmpty()) {
            docRef = events.document();
            event.setEventId(docRef.getId());
        } else {
            docRef = events.document(event.getEventId());
        }

        // Safety Check: Ensure organizerId is set before writing
        if (event.getOrganizerId() == null || event.getOrganizerId().isEmpty()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                event.setOrganizerId(currentUser.getUid());
                Log.d(TAG, "createEvent: Auto-assigned organizerId to current user: " + currentUser.getUid());
            } else {
                Log.w(TAG, "createEvent: Warning - Event created without an organizerId.");
            }
        }

        docRef.set(event)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Event successfully written: " + event.getEventTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Error writing event", e));

        return event.getEventId();
    }

    /**
     * Adds a user to the waitlist for an event.
     * Optionally stores the user's signup location if provided.
     * @param event The event to add the user to.
     * @param userId The user ID to add.
     * @param location The user's location at signup, or null if not required.
     */
    public void addUserToWaitlist(Event event, String userId, GeoPoint location){
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null or has no ID.");
            return;
        }

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

    /**
     * Removes a user from the waitlist for an event.
     * @param event The event to remove the user from.
     * @param userId The user ID to remove.
     */
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
        /**
         * Called when the user's location is successfully fetched.
         * @param location The user's GeoPoint location, or null if not available.
         */
        void onLocationFetched(GeoPoint location);

        /**
         * Called when an error occurs while fetching the location.
         * @param e The exception that occurred.
         */
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
     * Adds multiple users to the invited list for an event.
     * @param event The event to add users to.
     * @param userIds The list of user IDs to invite.
     */
    public void setInvitedUserIds(Event event, ArrayList<String> userIds) {
        if (event == null || event.getEventId() == null) {
            Log.e(TAG, "Event is null or has no ID.");
            return;
        }
        else {
            for (String userId : userIds) {
                // 1. Get the correct path: events -> {event-id} -> waitlistedUsers -> {user-id}
                DocumentReference invitedRef = events.document(event.getEventId())
                        .collection("invitedUsers").document(userId);
                // 2. Create a map to hold some data, like a timestamp.
                // Firestore documents cannot be completely empty.

                Map<String, Object> invitedData = new HashMap<>();
                invitedData.put("timestamp", FieldValue.serverTimestamp());

                // 3. Set the data. If the document already exists, this overwrites it but
                // that's fine. If it doesn't exist, it is created.
                invitedRef.set(invitedData)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "User " + userId + " successfully added to invited users for event " + event.getEventId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Error adding user to invited users", e));
            }


        }

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
        /**
         * Called when the list of invited user IDs is successfully fetched.
         * @param userIds The list of invited user IDs.
         */
        void onInvitedEntrantsFetched(List<String> userIds);

        /**
         * Called when an error occurs while fetching invited entrants.
         * @param e The exception that occurred.
         */
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
        /**
         * Called when the user's location is successfully fetched from the invited list.
         * @param location The user's GeoPoint location, or null if not available.
         */
        void onLocationFetched(GeoPoint location);

        /**
         * Called when an error occurs while fetching the location.
         * @param e The exception that occurred.
         */
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
        /**
         * Called when the list of invited locations is successfully fetched.
         * @param locations The list of GeoPoint locations.
         */
        void onInvitedLocationsFetched(List<GeoPoint> locations);

        /**
         * Called when an error occurs while fetching locations.
         * @param e The exception that occurred.
         */
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
        /**
         * Called when the list of cancelled user IDs is successfully fetched.
         * @param userIds The list of cancelled user IDs.
         */
        void onCancelledEntrantsFetched(List<String> userIds);

        /**
         * Called when an error occurs while fetching cancelled entrants.
         * @param e The exception that occurred.
         */
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
        /**
         * Called when the user's location is successfully fetched from the cancelled list.
         * @param location The user's GeoPoint location, or null if not available.
         */
        void onLocationFetched(GeoPoint location);

        /**
         * Called when an error occurs while fetching the location.
         * @param e The exception that occurred.
         */
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
        /**
         * Called when the list of cancelled locations is successfully fetched.
         * @param locations The list of GeoPoint locations.
         */
        void onCancelledLocationsFetched(List<GeoPoint> locations);

        /**
         * Called when an error occurs while fetching locations.
         * @param e The exception that occurred.
         */
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
     * Callback interface for fetching attending entrants.
     */
    public interface AttendingEntrantsCallback {
        /**
         * Called when the list of attending user IDs is successfully fetched.
         * @param userIds The list of attending user IDs.
         */
        void AttendingEntrantsFetched(List<String> userIds);

        /**
         * Called when an error occurs while fetching attending entrants.
         * @param e The exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Asynchronously fetches the list of user IDs from the selected list of a specific event.
     */
    public void getAttendingEntrants(String eventId, AttendingEntrantsCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty."));
            return;
        }
        events.document(eventId).collection("attendingUsers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> userIds = new ArrayList<>();
                    queryDocumentSnapshots.forEach(doc -> userIds.add(doc.getId()));
                    callback.AttendingEntrantsFetched(userIds);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Callback interface for fetching a user's location from the attending/selected list.
     */
    public interface UserLocationFromSelectedCallback {
        /**
         * Called when the user's location is successfully fetched from the selected list.
         * @param location The user's GeoPoint location, or null if not available.
         */
        void onLocationFetched(GeoPoint location);

        /**
         * Called when an error occurs while fetching the location.
         * @param e The exception that occurred.
         */
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
     * Callback interface for fetching attending/selected list locations.
     */
    public interface SelectedLocationsCallback {
        /**
         * Called when the list of selected locations is successfully fetched.
         * @param locations The list of GeoPoint locations.
         */
        void onSelectedLocationsFetched(List<GeoPoint> locations);

        /**
         * Called when an error occurs while fetching locations.
         * @param e The exception that occurred.
         */
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
