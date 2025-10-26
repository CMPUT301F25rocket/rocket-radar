package com.rocket.radar;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;

import java.util.Date;

class Time {
    int hour;
    int minute;
}

enum Section {
    GENERAL, DATETIME, DEADLINES, LOTTERY, STYLE;

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
}

class CreateEventModel extends ViewModel {
    // Creation wizard state.
    private Section section;

    // General
    private String title;
    private String description;


    private Boolean singleDayEvent;
    private Date eventDate;
    private Time eventStartTime;
    private Time eventEndTime;


    public Section getSection() {
        return section;
    }
}