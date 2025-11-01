package com.rocket.radar.profile;

import com.google.firebase.Timestamp;
import com.rocket.radar.events.Event;

import java.util.ArrayList;

public class ProfileModel {
    private String uid;
    private String name;
    private String phoneNumber;
    private String email;
    private Timestamp lastLogin;

    private ArrayList<Event> onWaitlistEvents;

    private ArrayList<Event> attendedEvents;

    private ArrayList<Event> pastEvents;

    public ProfileModel() {}

    public ProfileModel(String uid, String name, String email, String phoneNumber, Timestamp lastLogin) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.lastLogin = lastLogin;
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

    public ArrayList<Event> getOnWaitlistEvents() {
        return onWaitlistEvents;
    }

    public void setOnWaitlistEvents(ArrayList<Event> onWaitlistEvents) {
        this.onWaitlistEvents = onWaitlistEvents;
    }


    public ArrayList<Event> getAttendedEvents() {
        return attendedEvents;
    }

    public void setAttendedEvents(ArrayList<Event> attendedEvents) {
        this.attendedEvents = attendedEvents;
    }

    public ArrayList<Event> getPastEvents() {
        return pastEvents;
    }

    public void setPastEvents(ArrayList<Event> pastEvents) {
        this.pastEvents = pastEvents;
    }
}
