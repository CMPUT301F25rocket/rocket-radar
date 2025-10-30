package com.rocket.radar;

import com.google.firebase.database.Exclude; // Make sure this import is correct
import java.io.Serializable;

public class Event implements Serializable {
    String eventTitle;
    String date;
    String tagline;
    int image;

    // IMPORTANT: A public, no-argument constructor is required for Firebase.
    public Event() {
        // Default constructor required for calls to DataSnapshot.getValue(Event.class)
    }

    public Event(String eventTitle, String date, String tagline, int image) {
        this.eventTitle = eventTitle;
        this.date = date;
        this.tagline = tagline;
        this.image = image;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getDate() {
        return date;
    }

    public String getTagline() {
        return tagline;
    }

    // The @Exclude annotation tells Firebase to ignore this field when saving data.
    // This is crucial because the local resource ID is meaningless in the database.
    @Exclude
    public int getImage() {
        return image;
    }
}
