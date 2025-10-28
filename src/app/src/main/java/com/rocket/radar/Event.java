package com.rocket.radar;

public class Event {
    String eventTitle;
    String date;
    String tagline;
    int image;
    boolean onWaitlist;

    public Event(String eventTitle, String date, String tagline, int image, boolean onWaitlist) {
        this.eventTitle = eventTitle;
        this.date = date;
        this.tagline = tagline;
        this.image = image;
        this.onWaitlist = onWaitlist;
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

    public int getImage() {
        return image;
    }

    public boolean isOnWaitlist() {
        return onWaitlist;
    }
}