package com.rocket.radar.profile;

import com.google.firebase.Timestamp;
import com.rocket.radar.events.Event;

import java.util.ArrayList;

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

    private ArrayList<Event> onWaitlistEvents;

    private ArrayList<Event> attendedEvents;

    private ArrayList<Event> pastEvents;

    public ProfileModel() {}

    public ProfileModel(String uid, String name, String email, String phoneNumber, Timestamp lastLogin, Boolean notificationsEnabled, boolean geolocationEnabled, boolean isAdmin) {
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

    public Boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(Boolean admin) {
        isAdmin = admin;
    }

    public Boolean isGeolocationEnabled() {
        return geolocationEnabled;
    }

    public void setGeolocationEnabled(Boolean geolocationEnabled) {
        this.geolocationEnabled = geolocationEnabled;
    }

    public Boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(Boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public ArrayList<Event> getOnWaitlistEvents() {
        return onWaitlistEvents;
    }

    public void setOnWaitlistEvents(ArrayList<Event> onWaitlistEvents) {
        this.onWaitlistEvents = onWaitlistEvents;
    }

    public void addOnWaitlistEvent(Event event) {
        if (this.onWaitlistEvents == null) this.onWaitlistEvents = new ArrayList<>();
        this.onWaitlistEvents.add(event);
    }

    public ArrayList<Event> getAttendedEvents() {
        return attendedEvents;
    }

    public void setAttendedEvents(ArrayList<Event> attendedEvents) {
        this.attendedEvents = attendedEvents;
    }

    public void addAttendedEvent(Event event) {
        if (this.attendedEvents == null) this.attendedEvents = new ArrayList<>();
        this.attendedEvents.add(event);
    }


    public ArrayList<Event> getPastEvents() {
        return pastEvents;
    }

    public void setPastEvents(ArrayList<Event> pastEvents) {
        this.pastEvents = pastEvents;
    }

    public void addPastEvent(Event event) {
        if (this.pastEvents == null) this.pastEvents = new ArrayList<>();
        this.pastEvents.add(event);
    }
}
