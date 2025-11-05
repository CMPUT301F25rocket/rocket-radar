package com.rocket.radar.events;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

/**
 * Represents a user's check-in or signup for an event, including their location.
 */
public class CheckIn {
    private String userId;
    private String userName; // It's useful to store the name for easy display
    private GeoPoint signupLocation;
    @ServerTimestamp
    private Date signupTimestamp;

    public CheckIn() {
        // Required empty public constructor for Firestore deserialization
    }

    // Getters
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public GeoPoint getSignupLocation() { return signupLocation; }
    public Date getSignupTimestamp() { return signupTimestamp; }
}
