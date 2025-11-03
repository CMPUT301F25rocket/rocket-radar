package com.rocket.radar.profile;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

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
        if (profile.getOnWaitlistEvents() != null) {
            userMap.put("onWaitlistEvents", profile.getOnWaitlistEvents());
        }
        if (profile.getAttendedEvents() != null) {
            userMap.put("attendedEvents", profile.getAttendedEvents());
        }
        if (profile.getPastEvents() != null) {
            userMap.put("pastEvents", profile.getPastEvents());
        }
        db.collection("users")
                .document(profile.getUid())
                .set(userMap, SetOptions.merge()) // omitted fields remain untouched
                .addOnSuccessListener(aVoid -> {callback.onSuccess();;})
                .addOnFailureListener(callback::onError);
    }

    public void updateLastLogin(String uid) {
        db.collection("users")
                .document(uid)
                .update("lastLogin", FieldValue.serverTimestamp());
    }
}
