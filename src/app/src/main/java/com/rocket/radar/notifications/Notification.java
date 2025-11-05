package com.rocket.radar.notifications;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Notification {
    // These fields map directly to the document in the top-level 'notifications' collection
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

    public Notification() {
        // Default constructor required for calls to toObject(Notification.class)
    }

    // Getters
    public String getEventTitle() { return eventTitle; }
    public String getNotificationType() { return notificationType; }
    public boolean isReadStatus() { return readStatus; }
    public int getImage() { return image; }
    public Date getTimestamp() { return timestamp; }
    public String getNotificationId() { return notificationId; }
    public String getUserNotificationId() { return userNotificationId; }

    // Setters
    public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }
    public void setUserNotificationId(String userNotificationId) { this.userNotificationId = userNotificationId; }
}
