package com.rocket.radar;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.lifecycle.ViewModel;

import java.time.ZonedDateTime;
import java.util.Date;

class Time {
    int hour;
    int minute;
}

enum Section {
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

class CreateEventModel extends ViewModel implements Observable {
    // Other state
    private boolean showBottomSheet;

    public boolean getShowBottomSheet() {
        return showBottomSheet;
    }

    // Creation wizard state.
    private Section section;

    // General section field values
    private String title;
    private String description;


    // Datetime section field values
    private Boolean singleDayEvent;
    private Date eventDate;
    private Time eventStartTime;
    private Time eventEndTime;

    // Deadline section field values
    private Date registrationStartDate;
    private Date registrationEndDate;

    private Date initialSelectionStartDate;
    private Date initialSelectionEndDate;

    private Date finalAttendeeSelectionDate;

    // Lottery section field values
    private boolean hasWaitlistCapacity;
    private boolean hasLocationRequirement;
    private int waitlistCapacity;
    private int eventCapacity;
    private Date lotteryDate;
    private Time lotteryTime;

    // Style section field values
    private Bitmap image;
    private Color color;

    public CreateEventModel() {
        section = Section.GENERAL;
    }

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {

    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {

    }

    @Bindable
    public Section getSection() {
        return section;
    }

    @Bindable
    public int getLeftButtonVisibility() {
        if (section != Section.GENERAL) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @Bindable
    public int getRightButtonVisibility() {
        return View.VISIBLE;
    }

    @Bindable
    public String getRightButtonText() {
        if (section == Section.STYLE) return "Create";
        else return "Next";
    }

    public void rightButtonPress() {
        if (section == Section.lastSection) {
            // TODO: Create event
            return;
        } else {
            // Set the next section sequentially.
            section = Section.values()[section.ordinal() + 1];
        }
    }

    public void backSection() {

    }
}