package com.rocket.radar.notifications;

public class Notification {
    String eventTitle;
    String notificationType;
    boolean readStatus;
    int image;

    public Notification() {
        // Default constructor required for calls to toObject(Notification.class)
    }

    public Notification(String eventTitle, String notificationType, boolean readStatus, int image) {
        this.eventTitle = eventTitle;
        this.notificationType = notificationType;
        this.readStatus = readStatus;
        this.image = image;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public boolean isReadStatus() {
        return readStatus;
    }

    public int getImage() {
        return image;
    }
}
