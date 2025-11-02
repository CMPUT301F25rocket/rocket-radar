package com.rocket.radar.eventmanagement;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

public class CreateEventModel extends ViewModel {
    // Other state
    private boolean showBottomSheet;

    public boolean getShowBottomSheet() {
        return showBottomSheet;
    }

    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");

    // Creation wizard state.
    private MutableLiveData<Section> section;

    // General section field values
    public MutableLiveData<String> title;
    public MutableLiveData<String> description;


    // Datetime section field values
    public MutableLiveData<Boolean> singleDayEvent;
    public MutableLiveData<Optional<Date>> eventDate;

    public LiveData<String> eventDateDisplay() {
        return Transformations.map(eventDate, date -> date.map(dateFormatter::format).orElse(""));
    }

    public MutableLiveData<Optional<Time>> eventStartTime;
    public MutableLiveData<Optional<Time>> eventEndTime;

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
        section = new MutableLiveData<>(Section.GENERAL);
        singleDayEvent = new MutableLiveData<>(true);
        eventDate = new MutableLiveData<>(Optional.empty());
        eventStartTime = new MutableLiveData<>(Optional.empty());
        eventEndTime = new MutableLiveData<>(Optional.empty());
    }

    public LiveData<Section> getSection() {
        return section;
    }

    public int getLeftButtonVisibility() {
        if (section.getValue() != Section.GENERAL) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    public int getRightButtonVisibility() {
        return View.VISIBLE;
    }

    public String getRightButtonText() {
        if (section.getValue() == Section.STYLE) return "Create";
        else return "Next";
    }

    public void nextSection() {
        Section current = section.getValue();
        if (current == Section.lastSection) {
            return;
        } else {
            section.setValue(Section.values()[current.ordinal() + 1]);
        }
    }

    public void prevSection() {
        Section current = section.getValue();
        if (current== Section.firstSection) {
            return;
        } else {
            section.setValue(Section.values()[current.ordinal() - 1]);
        }
    }
}