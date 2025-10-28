package com.rocket.radar;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

/**
 * Represents a single notification document in Firestore.
 * This class is a POJO (Plain Old Java Object) used by Firestore for automatic data mapping.
 * Each field corresponds to a key in the Firestore document.
 * <p>
 * Author: Braden Woods
 */
@IgnoreExtraProperties // This is a crucial annotation. It tells Firestore to ignore any fields
// in the database document that don't have a matching field in this Java
// class. This prevents crashes if your data model changes.
public class Notification {

    // =====================================================================================
    // Fields Stored in Firestore
    // =====================================================================================

    private String title;          // The main headline of the notification (e.g., "You're In!").
    private String description;    // The body text providing more detail (e.g., "Congratulations! You have been selected...").
    private String eventId;        // The ID of the event this notification is associated with, used for navigation or fetching event details.
    private String type;           // A string representation of the NotificationType enum (e.g., "SELECTED_ENTRANTS").
    private String recipients;     // A string representation of the RecipientGroup enum (e.g., "selected").
    private String senderId;       // The user ID of the person who sent the notification (e.g., an organizer).
    private String imageUrl;       // A URL pointing to an image (e.g., event poster) to be displayed in the notification card.
    private boolean isRead = false;// A boolean to track if the user has opened/clicked the notification. Defaults to false for new notifications.

    @ServerTimestamp // This annotation tells Firestore to automatically populate this field with the
    // server's timestamp when the document is first created. This is reliable
    // and prevents issues with incorrect device times.
    private Timestamp timestamp;


    // =====================================================================================
    // Fields NOT Stored in Firestore (Local App Use Only)
    // =====================================================================================

    private String id;             // This holds the unique ID of the Firestore document itself.
    // It's not part of the document's data, so we must manually set
    // this field *after* we read the document from Firestore.


    // =====================================================================================
    // Constructors
    // =====================================================================================

    /**
     * An empty constructor is REQUIRED for Firestore's automatic data mapping to work.
     * When Firestore reads a document, it creates an instance of this class using this
     * constructor and then uses the public setters to populate the fields.
     */
    public Notification() {
        // This constructor must be public and empty.
    }

    /**
     * A convenience constructor for creating a new notification object from within the app code.
     * This is used by the NotificationController to build a notification before sending it.
     */
    public Notification(String title, String description, String eventId, String type,
                        String recipients, String senderId, String imageUrl) {
        this.title = title;
        this.description = description;
        this.eventId = eventId;
        this.type = type;
        this.recipients = recipients;
        this.senderId = senderId;
        this.imageUrl = imageUrl;
        // 'isRead' defaults to false, and 'timestamp' is set by the server, so they aren't needed here.
    }


    // =====================================================================================
    // Getters and Setters
    // Public getters and setters for all stored fields are REQUIRED for Firestore mapping.
    // =====================================================================================

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getRecipients() { return recipients; }
    public void setRecipients(String recipients) { this.recipients = recipients; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    /**
     * Gets the unique ID of the Firestore document. This field is not stored in the
     * document's data itself and must be manually set after fetching.
     * @return The Firestore document ID.
     */
    @Exclude // This annotation tells Firestore to IGNORE this getter during write operations.
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }


    // =====================================================================================
    // Convenience Methods for Type-Safety
    // These helpers convert raw strings to enums, preventing crashes from invalid data.
    // =====================================================================================

    /**
     * Safely converts the 'type' string into its corresponding NotificationType enum.
     * This is used in the UI layer (e.g., NotificationFragment) to decide what to do
     * when a notification is clicked. Marked with @Exclude for local logic.
     * @return The NotificationType enum, or a safe default (GENERIC_MESSAGE) if the string is invalid.
     */
    @Exclude
    public NotificationType getTypeEnum() {
        try {
            return NotificationType.valueOf(type);
        } catch (IllegalArgumentException | NullPointerException e) {
            // If the 'type' string from Firestore is null or doesn't match an enum,
            // return a safe default to prevent the app from crashing.
            return NotificationType.GENERIC_MESSAGE;
        }
    }

    /**
     * A helper to set the notification type using the type-safe enum.
     */
    @Exclude
    public void setTypeEnum(NotificationType t) {
        this.type = (t != null) ? t.name() : null;
    }

    /**
     * Safely converts the 'recipients' string into its corresponding RecipientGroup enum.
     */
    @Exclude
    public RecipientGroup getRecipientGroupEnum() {
        try {
            return RecipientGroup.valueOf(recipients.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Return a safe default if the string is invalid.
            return RecipientGroup.ALL;
        }
    }

    /**
     * A helper to set the recipient group using the type-safe enum.
     */
    @Exclude
    public void setRecipientGroupEnum(RecipientGroup g) {
        this.recipients = (g != null) ? g.name().toLowerCase() : null;
    }
}
