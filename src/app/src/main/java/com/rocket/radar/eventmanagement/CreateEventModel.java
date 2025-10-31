package com.rocket.radar.eventmanagement;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

import androidx.databinding.Bindable;
import androidx.databinding.Observable;
import androidx.lifecycle.ViewModel;

import java.util.Date;

public class CreateEventModel extends ViewModel implements Observable {
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

    public void nextSection() {
        if (section == Section.lastSection) {
            return;
        } else {
            section = Section.values()[section.ordinal() + 1];
        }
    }

    public void prevSection() {
        if (section == Section.firstSection) {
            return;
        } else {
            section = Section.values()[section.ordinal() - 1];
        }
    }

    public void backSection() {

    }
}