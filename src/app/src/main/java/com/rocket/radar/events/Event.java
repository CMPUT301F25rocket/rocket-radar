package com.rocket.radar.events;

import java.util.UUID;
import com.google.firebase.firestore.Exclude; // CORRECT: Using the Firestore Exclude
import java.io.Serializable;

/**
 * Represents a single event in the application.
 *
 * This class serves as a data model for event information, including its title, date,
 * and a unique identifier. It is designed to be serialized for passing between
 * Android components (e.g., Fragments) and is compatible with Google Firestore for
 * database operations. The {@link Serializable} interface allows Event objects to be
 * passed in Bundles.
 */
public class Event implements Serializable {
    /**
     * The unique identifier for the event. This is typically a UUID.
     */
    private String eventId; // ADD THIS FIELD
    /**
     * The main title or name of the event.
     */
    String eventTitle;
    /**
     * The formatted date string for when the event occurs (e.g., "30\nSEP").
     */
    String date;
    /**
     * A short, catchy description or subtitle for the event.
     */
    String tagline;
    /**
     * The local resource ID (e.g., {@code R.drawable.my_image}) for the event's banner image.
     * This field is excluded from Firestore serialization.
     */
    int image;

    /**
     * Default constructor required for Firestore data mapping.
     * Should not be used directly in code.
     */
    public Event() {
        // Default constructor required
    }

    /**
     * Constructs a new Event with specified details.
     * A unique {@code eventId} is automatically generated using {@link UUID}.
     *
     * @param eventTitle The title of the event.
     * @param date The date of the event.
     * @param tagline A short tagline for the event.
     * @param image The drawable resource ID for the event's image.
     */
    public Event(String eventTitle, String date, String tagline, int image) {
        this.eventId = UUID.randomUUID().toString(); // Generate a unique ID
        this.eventTitle = eventTitle;
        this.date = date;
        this.tagline = tagline;
        this.image = image;
    }

    // Standard getters
    /**
     * Gets the title of the event.
     * @return The event title as a String.
     */
    public String getEventTitle() { return eventTitle; }
    /**
     * Gets the date of the event.
     * @return The event date as a String.
     */
    public String getDate() { return date; }
    /**
     * Gets the tagline of the event.
     * @return The event tagline as a String.
     */
    public String getTagline() { return tagline; }
    /**
     * Gets the unique identifier of the event.
     * @return The event ID as a String.
     */
    public String getEventId() { return eventId; }

    /**
     * Sets the unique identifier for the event. This is primarily used by Firestore
     * during deserialization.
     * @param eventId The unique identifier string.
     */
    public void setEventId(String eventId) { this.eventId = eventId; }

    /**
     * Gets the local drawable resource ID for the event's image.
     * This method is annotated with {@link Exclude} to prevent the {@code image}
     * field from being serialized and stored in Firestore, as it is a local resource identifier.
     * @return The integer ID of the drawable resource.
     */
    @com.google.firebase.firestore.Exclude
    public int getImage() { return image; }
}
