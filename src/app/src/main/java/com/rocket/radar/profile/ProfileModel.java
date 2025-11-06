package com.rocket.radar.profile;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.rocket.radar.events.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user profile in the application.
 * This class is a data model that holds all information related to a user,
 * including their personal details, authentication information, and event-related activities.
 * It is designed to be easily serialized and deserialized, for example,
 * when interacting with a Firestore database.
 */
public class ProfileModel {
    private String uid;
    private String name;
    private String phoneNumber;
    private String email;
    private Timestamp lastLogin;

    private Boolean notificationsEnabled, geolocationEnabled, isAdmin;

    private ArrayList<String> onWaitlistEventIds;
    private ArrayList<String> onMyEventIds;

    public ProfileModel() {}

    public ProfileModel(String uid, String name, String email, String phoneNumber, Timestamp lastLogin, boolean notificationsEnabled, boolean geolocationEnabled, boolean isAdmin) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.lastLogin = lastLogin;
        this.notificationsEnabled = notificationsEnabled;
        this.geolocationEnabled = geolocationEnabled;
        this.isAdmin = isAdmin;
    }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Timestamp getLastLogin() { return lastLogin; }
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }

    // --- START OF FIX ---
    public Boolean isAdmin() {
        // If the value from Firestore is null, default to a safe value (false).
        if (isAdmin == null) {
            return false;
        }
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public Boolean isGeolocationEnabled() {
        // If the value from Firestore is null, default to a safe value (false).
        if (geolocationEnabled == null) {
            return false;
        }
        return geolocationEnabled;
    }

    public void setGeolocationEnabled(Boolean geolocationEnabled) {
        this.geolocationEnabled = geolocationEnabled;
    }

    public Boolean isNotificationsEnabled() {
        // If the value from Firestore is null, default to a safe value (true is a good default).
        if (notificationsEnabled == null) {
            return true;
        }
        return notificationsEnabled;
    }
    // --- END OF FIX ---

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public ArrayList<String> getOnWaitlistEventIds() {
        if (this.onWaitlistEventIds == null) this.onWaitlistEventIds = new ArrayList<>();
        return onWaitlistEventIds;
    }

    public ArrayList<String> getOnMyEventIds() {
        if (this.onMyEventIds == null) this.onMyEventIds = new ArrayList<>();
        return onMyEventIds;
    }

    public void setOnMyEventIds(ArrayList<String> onMyEventIds) {
        this.onMyEventIds = onMyEventIds;
    }


    public void setOnWaitlistEventIds(ArrayList<String> onWaitlistEventIds) {
        this.onWaitlistEventIds = onWaitlistEventIds;
    }

    public void addOnWaitlistEventId(String eventId) {
        if (this.onWaitlistEventIds == null) this.onWaitlistEventIds = new ArrayList<>();
        Log.d("Added to waitlist", "Event ID: " + eventId + "user: " + this.uid);
        this.onWaitlistEventIds.add(eventId);
    }
    public void addOnMyEventId(String eventId) {
        if (this.onMyEventIds == null) this.onMyEventIds = new ArrayList<>();
        Log.d("Added to my Events", "Event ID: " + eventId + "user: " + this.uid);
        this.onMyEventIds.add(eventId);
    }

    public void removeOnWaitlistEventId(String eventId) {
        if (this.onWaitlistEventIds == null) return;
        this.onWaitlistEventIds.remove(eventId);
    }
    public void removeOnMyEventId(String eventId) {
        if (this.onMyEventIds == null) return;
        this.onMyEventIds.remove(eventId);
    }

    public void clearOnWaitlistEventIds() {
        if (this.onWaitlistEventIds == null) return;
        this.onWaitlistEventIds.clear();
    }
}

