package com.rocket.radar.eventmanagement;

import androidx.annotation.NonNull;

import java.util.Date;

public class Time implements Comparable<Time> {
    private int hour;
    private int minute;

    public Time(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

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
}
