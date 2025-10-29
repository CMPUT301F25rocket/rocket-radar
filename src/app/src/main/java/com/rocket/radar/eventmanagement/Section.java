package com.rocket.radar;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Date;

public enum Section {
    // WARN: The order here is important. Section transitions will happen in the order these appear in.
    GENERAL, DATETIME, DEADLINES, LOTTERY, STYLE;

    public static final Section lastSection = Section.values()[Section.values().length - 1];

    // TODO: Pull these from strings.xml.

    /**
     *
     * @return Title for the section in the creation wizard and event editing UI
     * @throws IllegalArgumentException if the title has not yet been defined.
     */
    @NonNull
    public String getTitle() throws IllegalArgumentException {
        switch (this) {
            case GENERAL:
                return "General";
            case DATETIME:
                return "Date & Time";
            case DEADLINES:
                return "Event Deadlines";
            case LOTTERY:
                return "Lottery";
            case STYLE:
                return "Style";
        }
        throw new IllegalArgumentException("Section variant " + this + " has no title defined.");
    }

    public boolean isGeneralValid(String title, String description) {
        return !title.isBlank() && !description.isBlank();
    }

    public boolean isDatetimeValid(Date eventDate, Time eventStartTime, Time eventEndTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // return ZonedDateTime.from(eventDate).isAfter(ZonedDateTime.now())
        }
        return true;
    }
}
