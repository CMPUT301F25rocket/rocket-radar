package com.rocket.radar.eventmanagement;

import android.os.Build;

import androidx.annotation.NonNull;

import java.util.Date;

/**
 * Enumeration of sections in the event creation/editing wizard.
 * The order of these enum values determines the sequence of the wizard steps.
 */
public enum Section {
    // WARN: The order here is important. Section transitions will happen in the order these appear in.
    /** General event information section (title, description, location, etc.) */
    GENERAL,
    /** Lottery and capacity details section (waitlist, dates, etc.) */
    LOTTERY;

    /** The first section in the wizard */
    public static final Section firstSection = Section.values()[0];
    /** The last section in the wizard */
    public static final Section lastSection = Section.values()[Section.values().length - 1];

    // TODO: Pull these from strings.xml.

    /**
     * Returns the display title for this section.
     * @return Title for the section in the creation wizard and event editing UI
     * @throws IllegalArgumentException if the title has not yet been defined.
     */
    @NonNull
    public String getTitle() throws IllegalArgumentException {
        switch (this) {
            case GENERAL:
                return "General";
            case LOTTERY:
                return "Details";
        }
        throw new IllegalArgumentException("Section variant " + this + " has no title defined.");
    }

    /**
     * Validates the general section inputs.
     * @param title The event title to validate.
     * @param description The event description to validate.
     * @return true if both title and description are non-blank, false otherwise.
     */
    public boolean isGeneralValid(String title, String description) {
        return !title.isBlank() && !description.isBlank();
    }

    /**
     * Validates the datetime section inputs.
     * @param eventDate The event date to validate.
     * @param eventStartTime The event start time to validate.
     * @param eventEndTime The event end time to validate.
     * @return true if the datetime information is valid (currently always returns true).
     */
    public boolean isDatetimeValid(Date eventDate, Time eventStartTime, Time eventEndTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // return ZonedDateTime.from(eventDate).isAfter(ZonedDateTime.now())
        }
        return true;
    }
}
