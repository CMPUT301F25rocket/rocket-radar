package com.rocket.radar.events;

import com.google.firebase.firestore.Exclude; // CORRECT: Using the Firestore Exclude
import java.io.Serializable;

public class Event implements Serializable {
    private String eventId; // ADD THIS FIELD
    String eventTitle;
    String date;
    String tagline;
    int image;

    public Event() {
        // Default constructor required
    }

    public Event(String eventTitle, String date, String tagline, int image) {
        this.eventId = eventTitle; // Set the ID from the title
        this.eventTitle = eventTitle;
        this.date = date;
        this.tagline = tagline;
        this.image = image;
    }

    // Standard getters
    public String getEventTitle() { return eventTitle; }
    public String getDate() { return date; }
    public String getTagline() { return tagline; }
    public String getEventId() { return eventId; } // REMOVE @Exclude

    // Setter for eventId is good practice
    public void setEventId(String eventId) { this.eventId = eventId; }

    // image can remain excluded
    @com.google.firebase.firestore.Exclude
    public int getImage() { return image; }
}