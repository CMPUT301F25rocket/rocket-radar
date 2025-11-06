package com.rocket.radar.profile;

import android.util.Log;

import com.google.firebase.Firebase;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

//cite: general design was based on https://developer.android.com/topic/architecture, to separate the data layer in the architecture from the ui (view model)
public class ProfileRepository {

    private static final String TAG = "ProfileRepository";
    private final FirebaseFirestore db;

    // dependency injection
    public ProfileRepository(FirebaseFirestore db) {
        this.db = db;
    }

    public interface ReadCallback {
        void onProfileLoaded(ProfileModel profile);
        void onError(Exception e);
    }

    public interface WriteCallback {
        void onSuccess();
        void onError(Exception e);
    }
    public void readProfile(String uid, ReadCallback callback) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot ->
                        callback.onProfileLoaded(snapshot.toObject(ProfileModel.class)))
                .addOnFailureListener(callback::onError);
    }
    public void writeProfile(ProfileModel profile, WriteCallback callback) {

        // if you don't want to update something, assign the field to null
        // then it won't get sent to firestore
        Map<String, Object> userMap = new HashMap<>();
        if (profile.getName() != null) userMap.put("name", profile.getName());
        if (profile.getEmail() != null) userMap.put("email", profile.getEmail());
        if (profile.getPhoneNumber() != null) userMap.put("phoneNumber", profile.getPhoneNumber());
        if (profile.isNotificationsEnabled() != null) userMap.put("notificationsEnabled", profile.isNotificationsEnabled());
        if (profile.isGeolocationEnabled() != null) userMap.put("geolocationEnabled", profile.isGeolocationEnabled());
        if (profile.getOnWaitlistEventIds() != null) {
            userMap.put("onWaitlistEventIds", profile.getOnWaitlistEventIds());
        }
        if (profile.getOnMyEventIds() != null) {
            userMap.put("onMyEventIds", profile.getOnMyEventIds());
        }
        db.collection("users")
                .document(profile.getUid())
                .set(userMap, SetOptions.merge()) // omitted fields remain untouched
                .addOnSuccessListener(aVoid -> {callback.onSuccess();;})
                .addOnFailureListener(callback::onError);
    }

    public void deleteAccount(FirebaseUser user, ProfileModel profile, WriteCallback callback) {
        if (user == null) {
            callback.onError(new Exception("No authenticated user."));
            return;
        }
        // delete user firestore
        db.collection("users")
                .document(profile.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {callback.onSuccess();;})
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete Firestore document", e);
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


    public void updateLastLogin(String uid) {
        db.collection("users")
                .document(uid)
                .update("lastLogin", FieldValue.serverTimestamp());
    }
}
