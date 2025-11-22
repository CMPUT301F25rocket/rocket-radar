package com.rocket.radar.profile;

import android.util.Log;
import android.widget.AutoCompleteTextView;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Represents a user profile in the application.
 * This class is a data model that holds all information related to a user,
 * including their personal details, authentication information, and event-related activities.
 * It is designed to be easily serialized and deserialized, for example,
 * when interacting with a Firestore database.
 */
public class ProfileModel implements Serializable {
    private String uid;
    private String name;
    private String phoneNumber;
    private String email;
    private transient Timestamp lastLogin;
    private Boolean notificationsEnabled, geolocationEnabled;

    private ArrayList<String> onWaitlistEventIds;
    private ArrayList<String> onMyEventIds;
    private ArrayList<String> onInvitedEventIds;

    private String role;
    private ArrayList<String> attendingEventIds;
    private ArrayList<String> cancelledEventIds;

    public GeoPoint getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void setLastKnownLocation(GeoPoint lastKnownLocation) {
        this.lastKnownLocation = lastKnownLocation;
    }

    private transient GeoPoint lastKnownLocation;


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
     * @param role the role of the user
     */
    public ProfileModel(
            String uid,
            String name,
            String email,
            String phoneNumber,
            Timestamp lastLogin,
            boolean notificationsEnabled,
            boolean geolocationEnabled,
            UserRole role
    ) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.lastLogin = lastLogin;
        this.notificationsEnabled = notificationsEnabled;
        this.geolocationEnabled = geolocationEnabled;
        this.role = (role != null) ? role.name() : UserRole.ORGANIZER.name();
    }

    /**
     * Returns an ArrayList of event IDs to which the user is invited.
     * @return an ArrayList of invited event IDs
     */
    public ArrayList<String> getOnInvitedEventIds() {
        if (this.onInvitedEventIds == null) this.onInvitedEventIds = new ArrayList<>();
        return onInvitedEventIds;
    }

    /**
     * Sets the ArrayList of event IDs to which the user is invited.
     * @param onInvitedEventIds the new ArrayList to set for invited event IDs
     */
    public void setOnInvitedEventIds(ArrayList<String> onInvitedEventIds) {
        this.onInvitedEventIds = onInvitedEventIds;
    }

    public void addOnInvitedEventId(String eventId) {
        if (this.onInvitedEventIds == null) this.onInvitedEventIds = new ArrayList<>();
        this.onInvitedEventIds.add(eventId);
    }

    /**
     * Returns an ArrayList of event IDs to which the user is invited.
     * @return an ArrayList of invited event IDs
     */
    public ArrayList<String> getAttendingEventIds() {
        if (this.attendingEventIds == null) this.attendingEventIds = new ArrayList<>();
        return attendingEventIds;
    }

    /**
     * Sets the ArrayList of event IDs to which the user is invited.
     * @param attendingEventIds the new ArrayList to set for invited event IDs
     */
    public void setAttendingEventIds(ArrayList<String> attendingEventIds) {
        this.attendingEventIds = attendingEventIds;
    }

    public void addAttendingEventId(String eventId) {
        if (this.attendingEventIds == null) this.attendingEventIds = new ArrayList<>();
        this.attendingEventIds.add(eventId);
    }

    public void removeInvitedEventId(String eventId) {
        if (this.onInvitedEventIds == null) return;
        this.onInvitedEventIds.remove(eventId);

    }

    public ArrayList<String> getCancelledEventIds() {
        if (this.cancelledEventIds == null) this.cancelledEventIds = new ArrayList<>();
        return cancelledEventIds;
    }

    /**
     * Sets the ArrayList of event IDs to which the user is invited.
     * @param cancelledEventIds the new ArrayList to set for invited event IDs
     */
    public void setCancelledEventIds(ArrayList<String> cancelledEventIds) {
        this.cancelledEventIds = cancelledEventIds;
    }

    public void addCancelledEventId(String eventId) {
        if (this.cancelledEventIds == null) this.cancelledEventIds = new ArrayList<>();
        this.cancelledEventIds.add(eventId);
    }



    /**
     * Represents the permissions a user has.
     */
    public enum UserRole {
        ENTRANT,
        ORGANIZER,
        ADMIN
    }

    /**
     * Gets the permissions of a user.
     * @return the role the user has
     */
    public UserRole getRole() {
        if (role == null) {
            return UserRole.ORGANIZER; // default to org
        }
        try {
            return UserRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            return UserRole.ORGANIZER;
        }
    }

    /**
     * Sets the permissions of a user.
     * @param role the role to set
     */
    public void setRole(UserRole role) {
        if (role != null)
            this.role = role.name();
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

