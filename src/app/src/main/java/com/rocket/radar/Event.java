package com.rocket.radar;

import java.io.Serializable;

public class Event implements Serializable {
    String eventTitle;
    String date;
    String tagline;
    int image;

    public Event() {
        // Default constructor required for calls to toObject(Event.class)
    }

    public Event(String eventTitle, String date, String tagline, int image) {
        this.eventTitle = eventTitle;
        this.date = date;
        this.tagline = tagline;
        this.image = image;
    }

    // Getters are required for Firebase to serialize the object
    public String getEventTitle() { return eventTitle; }
    public String getDate() { return date; }
    public String getTagline() { return tagline; }

    // Exclude the local image resource ID from being saved to Firestore
    @Exclude
    public int getImage() { return image; }
}
