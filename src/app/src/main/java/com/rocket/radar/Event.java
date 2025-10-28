package com.rocket.radar;

import java.util.List;

public class Event {
    private long time;
    private String name;
    private int thumbnail;
    private List<User> waitlist;
    private boolean onWaitlist;

    public Event(long time, String name, int thumbnail, List<User> waitlist, boolean onWaitlist) {
        this.time = time;
        this.name = name;
        this.thumbnail = thumbnail;
        this.waitlist = waitlist;
        this.onWaitlist = onWaitlist;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(int thumbnail) {
        this.thumbnail = thumbnail;
    }

    public List<User> getWaitlist() {
        return waitlist;
    }

    public void setWaitlist(List<User> waitlist) {
        this.waitlist = waitlist;
    }

    public boolean isOnWaitlist() {
        return onWaitlist;
    }

    public void setOnWaitlist(boolean onWaitlist) {
        this.onWaitlist = onWaitlist;
    }

    @Override
    public String toString() {
        return "Event{" +
                "time=" + time +
                ", name='" + name + '\'' +
                ", thumbnail=" + thumbnail +
                ", waitlist=" + waitlist +
                ", onWaitlist=" + onWaitlist +
                '}';
    }
}
