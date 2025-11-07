package com.rocket.radar.notifications;

/**
 * Represents a single notification item. This class is a model for data retrieved from
 * the Firebase Firestore database. It includes both data stored in the top-level 'notifications'
 * collection and user-specific state data (like read status) from a user's sub-collection.
 *
 * Outstanding Issues:
 * - The `image` field is an integer resource ID, which might be better handled as a String
 *   URL or identifier if images are to be fetched from a remote source in the future.
 */
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notification {
    // These fields map directly to the document in the top-level 'notifications' collection
    // This is the main title or message of the event.
    private String eventTitle;
    private String notificationType;
    private int image;
    @ServerTimestamp // Firestore will automatically populate this on the server
    private Date timestamp;

    // These fields are for UI state and are populated manually in the repository
    @DocumentId // This will automatically be populated with the document's ID
    private String notificationId;
    private String userNotificationId; // The ID of the document in the user's sub-collection
    private boolean readStatus;

    /**
     * Default constructor required for Firestore's toObject() method.
     */
    public Notification() {
        // Default constructor required for calls to toObject(Notification.class)
    }

    // Getters
    /**
     * Gets the main title of the notification.
     * @return The notification title string.
     */
    public String getEventTitle() { return eventTitle; }

    /**
     * Gets the type of the notification (e.g., "milestone", "announcement").
     * @return The notification type string.
     */
    public String getNotificationType() { return notificationType; }

    /**
     * Gets the read status of the notification for the current user.
     * @return true if the notification has been read, false otherwise.
     */
    public boolean isReadStatus() { return readStatus; }

    /**
     * Gets the drawable resource ID for the notification's icon.
     * @return The integer ID of the drawable resource.
     */
    public int getImage() { return image; }

    /**
     * Gets the server-generated timestamp of when the notification was created.
     * @return A Date object representing the creation time.
     */
    public Date getTimestamp() { return timestamp; }

    /**
     * Gets the unique ID of the notification document from the top-level 'notifications' collection.
     * This is automatically populated by Firestore.
     * @return The document ID string.
     */
    public String getNotificationId() { return notificationId; }

    /**
     * Gets the unique ID of the user-specific notification document, which stores the read status.
     * @return The user-specific notification document ID string.
     */
    public String getUserNotificationId() { return userNotificationId; }

    // Setters
    /**
     * Sets the read status of the notification.
     * @param readStatus The new read status (true for read, false for unread).
     */
    public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }

    /**
     * Sets the ID of the user-specific notification document. This is used to link
     * the general notification data with the user's personal read status.
     * @param userNotificationId The ID of the document in the user's notification sub-collection.
     */
    public void setUserNotificationId(String userNotificationId) { this.userNotificationId = userNotificationId; }
}
