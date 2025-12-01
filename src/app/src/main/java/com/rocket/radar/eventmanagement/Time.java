package com.rocket.radar.eventmanagement;

import androidx.annotation.NonNull;

import java.util.Date;

/**
 * Represents a time of day with hour and minute components.
 * This class implements Comparable to allow time comparisons.
 */
public class Time implements Comparable<Time> {
    private int hour;
    private int minute;

    /**
     * Constructs a Time with the specified hour and minute.
     * @param hour The hour of the day (0-23).
     * @param minute The minute of the hour (0-59).
     */
    public Time(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    /**
     * Constructs a Time set to midnight (00:00).
     */
    public Time() {
        hour = 0;
        minute = 0;
    }

    /**
     * Gets the hour component of this time.
     * @return The hour (0-23).
     */
    public int getHour() {
        return hour;
    }

    /**
     * Sets the hour of this time. If out of range the hour will be wrapped.
     * @param hour Some hour of date between 0 and 23.
     */
    public void setHour(int hour) {
        this.hour = hour % 24;
    }

    /**
     * Gets the minute component of this time.
     * @return The minute (0-59).
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Sets the minute of this time. If out of range the minute will be wrapped.
     * @param minute Some hour of date between 0 and 59.
     */
    public void setMinute(int minute) {
        this.minute = minute % 60;
    }

    @NonNull
    @Override
    public String toString() {
        return Integer.toString(hour) + ":" + String.format("%02d", minute);
    }


    @Override
    public int compareTo(Time o) {
        if (hour < o.hour) {
            return -1;
        } else if (hour > o.hour) {
            return 1;
        }
        if (minute < o.minute) {
            return -1;
        } else if (minute > o.minute) {
            return 1;
        }
        return 0;
    }

    /**
     * Checks if this time is before another time.
     * @param o The time to compare against.
     * @return true if this time is before the specified time, false otherwise.
     */
    public boolean lessThan(Time o) {
        return this.compareTo(o) < 0;
    }
}
