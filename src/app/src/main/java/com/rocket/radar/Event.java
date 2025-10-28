package com.rocket.radar;

public class Event {
    String eventTitle;
    String date;
    String tagline;
    int image;

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

    public int getImage() {
        return image;
    }
}