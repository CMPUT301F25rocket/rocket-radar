package com.rocket.radar.eventmanagement;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.maxkeppeler.sheets.calendar.CalendarSheet;
import com.maxkeppeler.sheets.calendar.SelectionMode;
import com.maxkeppeler.sheets.clock.ClockSheet;
import com.maxkeppeler.sheets.core.SheetStyle;
import com.rocket.radar.R;
import com.rocket.radar.databinding.ActivityCreateEventBinding;

import java.util.Date;
import java.util.Optional;

import kotlin.Unit;

/**
 * Activity that walks a user through filling out the various pieces of information needed to create
 * an event. This activity makes heavy use of view and databinding. Here are some relevant pieces of 
 * information:
 * - https://developer.android.com/reference/com/google/android/material/bottomsheet/BottomSheetBehavior
 * - https://stackoverflow.com/questions/55682256/android-set-bottom-sheet-state-in-xml
 * - https://medium.com/@mananwason/bottoms-sheets-in-android-280c03280072
 */
public class CreateEventActivity extends AppCompatActivity {
    ActivityCreateEventBinding binding;
    CreateEventModel model;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // These three lines took way too long to write. ʕノ•ᴥ•ʔノ ︵ ┻━┻
        // WARN: Make sure when you create variables you call setMyVarName(...) on the binding.
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        model = new CreateEventModel();
        binding.setCreateEvent(model);
        binding.setLifecycleOwner(this);

        binding.createEventGeneralSection.setCreateEvent(model);
        binding.createEventGeneralSection.setLifecycleOwner(this);

        binding.createEventDatetimeSection.setCreateEvent(model);
        binding.createEventDatetimeSection.setLifecycleOwner(this);


        // Main navigation buttons
        binding.createEventWizardNavLeftButton.setOnClickListener(btn -> {
            model.prevSection();
        });
        binding.createEventWizardNavRightButton.setOnClickListener(btn -> {
            model.nextSection();
        });


        setContentView(binding.getRoot());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Unfortunately it seems viewbinding fails for included views.
        // Now we need to bind the inputs that function by sheet.
        TextInputEditText pickEventDate = binding.getRoot().findViewById(R.id.inputEventDatetimeDaterangeTextInput);
        TextInputEditText pickEventStart = binding.getRoot().findViewById(R.id.inputEventDatetimeStartTextInput);
        TextInputEditText pickEventEnd = binding.getRoot().findViewById(R.id.inputEventDatetimeEndTextInput);

        pickEventDate.setOnClickListener(itxt -> {
            CalendarSheet calendarSheet = new CalendarSheet();
            calendarSheet.show(CreateEventActivity.this, null, sheet -> {
                sheet.style(SheetStyle.BOTTOM_SHEET);
                sheet.title("Start Date");
                sheet.rangeYears(7);
                sheet.selectionMode(SelectionMode.DATE);
                sheet.onPositive((start, end) -> {
                    Date date = start.getTime();
                    model.eventDate.setValue(Optional.of(start.getTime()));
                    return Unit.INSTANCE;
                });

                // Well the sheet library is written in kotlin so...
                return Unit.INSTANCE;
            });
        });

        pickEventStart.setOnClickListener(itxt -> {
            ClockSheet clockSheet = new ClockSheet();
            clockSheet.show(CreateEventActivity.this, null, sheet -> {
                sheet.style(SheetStyle.BOTTOM_SHEET);
                sheet.title("Start Time");

                sheet.onPositive((epoch, hours, minutes) -> {
                    model.eventStartTime.setValue(Optional.of(new Time(hours, minutes)));
                    return Unit.INSTANCE;
                });

                return Unit.INSTANCE;
            });
        });

        pickEventEnd.setOnClickListener(itxt -> {
            ClockSheet clockSheet = new ClockSheet();
            clockSheet.show(CreateEventActivity.this, null, sheet -> {
                sheet.style(SheetStyle.BOTTOM_SHEET);
                sheet.title("End Time");

                sheet.onPositive((epoch, hours, minutes) -> {
                    model.eventStartTime.setValue(Optional.of(new Time(hours, minutes)));
                    return Unit.INSTANCE;
                });

                return Unit.INSTANCE;
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
