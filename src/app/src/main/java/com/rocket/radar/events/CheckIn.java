package com.rocket.radar.events;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a single check-in record for an event.
 * This class is designed for use with Firestore, capturing who checked in, where, and when.
 */
public class CheckIn {

    private String userId;
    private String userName;
    private GeoPoint signupLocation;

    // --- START OF FIX: Add status field and getter ---
    private String status; // e.g., "attending", "waitlist"
    // --- END OF FIX ---

    @ServerTimestamp
    private Date timestamp;

    // Firestore requires a no-argument constructor
    public CheckIn() {}

    public CheckIn(String userId, String userName, GeoPoint signupLocation) {
        this.userId = userId;
        this.userName = userName;
        this.signupLocation = signupLocation;
        this.status = "attending"; // Default status
    }

    // --- Getters and Setters ---

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public GeoPoint getSignupLocation() {
        return signupLocation;
    }

    public void setSignupLocation(GeoPoint signupLocation) {
        this.signupLocation = signupLocation;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    // --- START OF FIX: Add getter for the status field ---
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    // --- END OF FIX ---
}
