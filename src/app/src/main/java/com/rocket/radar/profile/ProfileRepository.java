package com.rocket.radar.profile;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

//cite: general design was based on https://developer.android.com/topic/architecture, to separate the data layer in the architecture from the ui (view model) accessed: October 28, 2025

/**
 * Repository class that manages read, write and delete operations for the Firestore user collection.
 * It also manages adding event id's to collections stored in Firestore for waitlist and my events.
 */
public class ProfileRepository {

    private static final String TAG = "ProfileRepository";
    private final FirebaseFirestore db;

    /**
     * Constructor for a new ProfileRepository.
     */
    public ProfileRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Callback interface for reading profile data from Firestore.
     * Used to handle asynchronous read operations.
     */
    public interface ReadCallback {
        /**
         * Called when a user profile has been successfully loaded.
         * @param profile the loaded ProfileModel object
         */
        void onProfileLoaded(ProfileModel profile);

        /**
         * Called when an error occurs from a Firestore read.
         * @param e the exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Callback interface for writing profile data to Firestore.
     * Used to handle asynchronous write operations.
     */
    public interface WriteCallback {
        /**
         * Called when the write succeeds.
         */
        void onSuccess();

        /**
         * Called when an error occurs from a Firestore write.
         * @param e the exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * Reads a user profile document from Firestore using the uid.
     * @param uid The user id of the user.
     * @param callback A ReadCallback to handle success and failure of the async operation.
     */
    public void readProfile(String uid, ReadCallback callback) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onProfileLoaded(snapshot.toObject(ProfileModel.class)))
                .addOnFailureListener(callback::onError);
    }

    /**
     * Writes or updates a user profile document in Firestore.
     * @param profile the ProfileModel of the user.
     * @param callback A WriteCallback to handle success and failure of the async operation.
     */
    public void writeProfile(ProfileModel profile, WriteCallback callback) {

        // if you don't want to update something, assign the field to null
        // then it won't get sent to firestore
        Map<String, Object> userMap = new HashMap<>();
        if (profile.getName() != null) userMap.put("name", profile.getName());
        if (profile.getEmail() != null) userMap.put("email", profile.getEmail());
        if (profile.getPhoneNumber() != null) userMap.put("phoneNumber", profile.getPhoneNumber());
        if (profile.isNotificationsEnabled() != null) userMap.put("notificationsEnabled", profile.isNotificationsEnabled());
        if (profile.isGeolocationEnabled() != null) userMap.put("geolocationEnabled", profile.isGeolocationEnabled());
        if (profile.getRole() != null) userMap.put("role", profile.getRole());
        if (profile.getOnWaitlistEventIds() != null) {
            userMap.put("onWaitlistEventIds", profile.getOnWaitlistEventIds());
        }
        if (profile.getOnMyEventIds() != null) {
            userMap.put("onMyEventIds", profile.getOnMyEventIds());
        }
        if (profile.getOnInvitedEventIds() != null) {
            userMap.put("onInvitedEventIds", profile.getOnInvitedEventIds());
        }

        db.collection("users")
                .document(profile.getUid())
                .set(userMap, SetOptions.merge()) // omitted fields remain untouched
                .addOnSuccessListener(aVoid -> {callback.onSuccess();;})
                .addOnFailureListener(callback::onError);
    }

    /**
     * Deletes a users account by calling delete on the user document in Firestore.
     * @param user the FirebaseUser to delete.
     * @param profile the ProfileModel of the user to delete.
     * @param callback A WriteCallback to handle success and failure of the async operation.
     */
    public void deleteAccount(FirebaseUser user, ProfileModel profile, WriteCallback callback) {
        if (user == null) {
            callback.onError(new Exception("No authenticated user."));
            return;
        }

        String uid = profile.getUid();

        db.collection("users")
                .document(uid)
                .collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (var document : querySnapshot.getDocuments()) { // delete notification docs
                        document.getReference().delete();
                    }
                    db.collection("users") // delete user doc
                            .document(uid)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "User and notifications deleted successfully");
                                callback.onSuccess();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete Firestore user document", e);
                                callback.onError(e);
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve notifications for deletion", e);
                    callback.onError(e);
                });
    }

    /**
     * Atomically adds a new event ID to the user's waitlist in Firestore.
     * This uses FieldValue.arrayUnion to ensure the operation is atomic and avoids race conditions.
     *
     * @param uid The user's unique ID.
     * @param eventId The event ID to add to the waitlist.
     * @param callback A callback to handle success or failure.
     */
    public void addEventIdToWaitlist(String uid, String eventId, WriteCallback callback) {
        db.collection("users")
                .document(uid)
                .update("onWaitlistEventIds", FieldValue.arrayUnion(eventId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }

    /**
     * Atomically adds a new event ID to the user's MyEvents in Firestore.
     * This uses FieldValue.arrayUnion to ensure the operation is atomic and avoids race conditions.
     *
     * @param uid The user's unique ID.
     * @param eventId The event ID to add to the MyEvents.
     * @param callback A callback to handle success or failure.
     */
    public void addEventIdToMyEvent(String uid, String eventId, WriteCallback callback) {
        db.collection("users")
                .document(uid)
                .update("onMyEventIds", FieldValue.arrayUnion(eventId))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onError);
    }


    /**
     * Updates the last login in Firestore for the user.
     * @param uid The user id.
     */
    public void updateLastLogin(String uid) {
        db.collection("users")
                .document(uid)
                .update("lastLogin", FieldValue.serverTimestamp());
    }

    /**
     * Updates the location of the user in Firestore.
     * @param uid The user id.
     * @param location The location of the user.
     */
    public void updateUserProfileLocation(String uid, GeoPoint location) {
        if (uid == null || uid.isEmpty()) {
            Log.e("ProfileViewModel", "Cannot update location, UID is null or empty.");
            return;
        }
        db.collection("users").document(uid)
                .update("lastKnownLocation", location)
                .addOnSuccessListener(aVoid -> Log.d("ProfileViewModel", "User location successfully updated for UID: " + uid))
                .addOnFailureListener(e -> Log.e("ProfileViewModel", "Error updating user location for UID: " + uid, e));
    }
}
