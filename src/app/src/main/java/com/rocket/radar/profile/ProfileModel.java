package com.rocket.radar.profile;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
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

    public GeoPoint getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(GeoPoint lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    private GeoPoint lastKnownLocation;


    /**
     * Empty constructor required by Firestore
     */
    public ProfileModel() {}

    /**
     * Constructor that creates profile with info
     * @param uid user id
     * @param name name of user
     * @param email email of user
     * @param phoneNumber phone number of user
     * @param lastLogin last time they logged in
     * @param notificationsEnabled if notifications are on
     * @param geolocationEnabled if geolocation is on
     * @param isAdmin if the profile is for the moderator (NOT IMPLEMENTED YET)
     */
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

    /**
     * Returns the user id.
     * @return uid the user id to return
     */
    public String getUid() { return uid; }

    /**
     * Sets the user id.
     * @param uid the user id to set
     */
    public void setUid(String uid) { this.uid = uid; }

    /**
     * Returns the name of the user.
     * @return name of the user
     */
    public String getName() { return name; }

    /**
     * Sets name of the user.
     * @param name name of the user
     */
    public void setName(String name) { this.name = name; }

    /**
     * Returns the email of the user.
     * @return email of the user.
     */
    public String getEmail() { return email; }

    /**
     * Sets email of the user.
     * @param email email of the user to set.
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Returns phone number of the user.
     * @return phone number of the user.
     */
    public String getPhoneNumber() { return phoneNumber; }

    /**
     * Sets the phone number of the user.
     * @param phoneNumber the phone number of the user to set.
     */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /**
     * Returns the last time the user logged in.
     * @return A Timestamp, that is the last time the user logged in.
     */
    public Timestamp getLastLogin() { return lastLogin; }

    /**
     * Sets the last time the user logged in.
     * Probably not needed, but included for consistency.
     * @param lastLogin the Timestamp to set for the last time the user logged in.
     */
    public void setLastLogin(Timestamp lastLogin) { this.lastLogin = lastLogin; }


    /**
     * Returns whether the user is an Admin/Moderator.
     * @return Boolean, True if the user is an admin, false otherwise.
     */
    public Boolean isAdmin() {
        // If the value from Firestore is null, default to a safe value (false).
        if (isAdmin == null) {
            return false;
        }
        return isAdmin;
    }

    /**
     * Sets whether the user is an Admin/Moderator.
     * @param admin the Boolean to set whether the user is an Admin/Moderator or not.
     */
    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    /**
     * Returns whether the user has geolocation enabled or not.
     * @return True if the geolocation is Enabled, false otherwise.
     */
    public Boolean isGeolocationEnabled() {
        // If the value from Firestore is null, default to a safe value (false).
        if (geolocationEnabled == null) {
            return false;
        }
        return geolocationEnabled;
    }

    /**
     * Sets whether geolocation is enabled.
     * @param geolocationEnabled the Boolean to set whether geolocation should be enabled or not.
     */
    public void setGeolocationEnabled(Boolean geolocationEnabled) {
        this.geolocationEnabled = geolocationEnabled;
    }

    /**
     * Returns whether the user has notifications enabled or not.
     * @return True if the notifications are enabled, false otherwise.
     */
    public Boolean isNotificationsEnabled() {
        // If the value from Firestore is null, default to a safe value (true is a good default).
        if (notificationsEnabled == null) {
            return true;
        }
        return notificationsEnabled;
    }

    /**
     * Sets whether notifications is enabled.
     * @param notificationsEnabled the Boolean to set whether notifications should be enabled or not.
     */
    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    /**
     * Returns an ArrayList that is the waitlist event ids (the user joined the waitlist for these events)
     * @return an ArrayList of the waitlist event ids
     */
    public ArrayList<String> getOnWaitlistEventIds() {
        if (this.onWaitlistEventIds == null) this.onWaitlistEventIds = new ArrayList<>();
        return onWaitlistEventIds;
    }

    /**
     * Returns an ArrayList that is the my event ids (the ids for the events the user created)
     * @return an ArrayList of the my event ids
     */
    public ArrayList<String> getOnMyEventIds() {
        if (this.onMyEventIds == null) this.onMyEventIds = new ArrayList<>();
        return onMyEventIds;
    }

    /**
     * Sets an ArrayList that is the my event ids (the ids for the events the user created)
     * @param onMyEventIds the new ArrayList to set of my event ids
     */
    public void setOnMyEventIds(ArrayList<String> onMyEventIds) {
        this.onMyEventIds = onMyEventIds;
    }

    /**
     * Sets an ArrayList that is the my waitlist ids (the ids for the events the user joined the waitlist for)
     * @param onWaitlistEventIds the new ArrayList to set of waitlist event ids
     */
    public void setOnWaitlistEventIds(ArrayList<String> onWaitlistEventIds) {
        this.onWaitlistEventIds = onWaitlistEventIds;
    }

    /**
     * Adds an event id for a waitlisted event to the ArrayList
     * @param eventId the waitlist event id to add
     */
    public void addOnWaitlistEventId(String eventId) {
        if (this.onWaitlistEventIds == null) this.onWaitlistEventIds = new ArrayList<>();
        Log.d("Added to waitlist", "Event ID: " + eventId + "user: " + this.uid);
        this.onWaitlistEventIds.add(eventId);
    }

    /**
     * Adds an event id for a event the user created to the ArrayList
     * @param eventId the my event id to add
     */
    public void addOnMyEventId(String eventId) {
        if (this.onMyEventIds == null) this.onMyEventIds = new ArrayList<>();
        Log.d("Added to my Events", "Event ID: " + eventId + "user: " + this.uid);
        this.onMyEventIds.add(eventId);
    }

    /**
     * Removes an event id from the ArrayList of waitlist event ids.
     * @param eventId the waitlist event id to remove
     */
    public void removeOnWaitlistEventId(String eventId) {
        if (this.onWaitlistEventIds == null) return;
        this.onWaitlistEventIds.remove(eventId);
    }
    /**
     * Removes an event id from the ArrayList of my event ids.
     * @param eventId the my event id to remove
     */
    public void removeOnMyEventId(String eventId) {
        if (this.onMyEventIds == null) return;
        this.onMyEventIds.remove(eventId);
    }

    /**
     * Clears all event IDs from the waitlist.
     */
    public void clearOnWaitlistEventIds() {
        if (this.onWaitlistEventIds == null) return;
        this.onWaitlistEventIds.clear();
    }


}

