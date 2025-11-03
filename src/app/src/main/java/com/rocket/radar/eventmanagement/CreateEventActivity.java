package com.rocket.radar.eventmanagement;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.maxkeppeler.sheets.calendar.CalendarSheet;
import com.maxkeppeler.sheets.calendar.SelectionMode;
import com.maxkeppeler.sheets.clock.ClockSheet;
import com.maxkeppeler.sheets.color.ColorSheet;
import com.maxkeppeler.sheets.core.SheetStyle;
import com.rocket.radar.R;
import com.rocket.radar.databinding.ActivityCreateEventBinding;

import org.w3c.dom.Text;

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

        // Main navigation buttons
        binding.createEventWizardNavLeftButton.setOnClickListener(btn -> {
            model.prevSection();
        });
        binding.createEventWizardNavRightButton.setOnClickListener(btn -> {
            model.nextSection();
        });

        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);

        // Bind the model to the views.
        // FIXME: These should have been fragments, the model should have been stored in a provider
        // on the activity and then these lines could have been split between each fragment.
        binding.setCreateEvent(model);
        binding.setLifecycleOwner(this);

        binding.createEventGeneralSection.setCreateEvent(model);
        binding.createEventGeneralSection.setLifecycleOwner(this);

        binding.createEventDatetimeSection.setCreateEvent(model);
        binding.createEventDatetimeSection.setLifecycleOwner(this);

        binding.createEventDeadlineSection.setCreateEvent(model);
        binding.createEventDeadlineSection.setLifecycleOwner(this);

        binding.createEventLotterySection.setCreateEvent(model);
        binding.createEventLotterySection.setLifecycleOwner(this);

        binding.createEventStyleSection.setCreateEvent(model);
        binding.createEventStyleSection.setLifecycleOwner(this);
    }

    private void openCalendarBottomSheet(MutableLiveData<Optional<Date>> sink, View view) {
        CalendarSheet calendarSheet = new CalendarSheet();
        calendarSheet.show(CreateEventActivity.this, null, sheet -> {
            sheet.style(SheetStyle.BOTTOM_SHEET);
            sheet.rangeYears(7);
            sheet.selectionMode(SelectionMode.DATE);
            sheet.displayToolbar(false);
            sheet.onPositive((start, end) -> {
                Date date = start.getTime();
                sink.setValue(Optional.of(start.getTime()));
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onNegative(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onClose(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            // Well the sheet library is written in kotlin so...
            return Unit.INSTANCE;
        });
    }

    private void openTimeBottomSheet(MutableLiveData<Optional<Time>> sink, View view) {
        ClockSheet clockSheet = new ClockSheet();
        clockSheet.show(CreateEventActivity.this, null, sheet -> {
            sheet.style(SheetStyle.BOTTOM_SHEET);
            sheet.displayToolbar(false);

            sheet.onPositive((epoch, hours, minutes) -> {
                sink.setValue(Optional.of(new Time(hours, minutes)));
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onNegative(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            sheet.onClose(() -> {
                view.clearFocus();
                return Unit.INSTANCE;
            });

            return Unit.INSTANCE;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Unfortunately it seems viewbinding fails for included views.
        // Now we need to bind the inputs that function by sheet.

        // Datetime section bindings.
        TextInputEditText pickEventDate = binding.getRoot().findViewById(R.id.inputEventDatetimeDaterangeTextInput);
        pickEventDate.setOnFocusChangeListener((view, hasFocus)-> {
            if (hasFocus) openCalendarBottomSheet(model.eventDate, view);
        });

        TextInputEditText pickEventStart = binding.getRoot().findViewById(R.id.inputEventDatetimeStartTextInput);
        pickEventStart.setOnFocusChangeListener((view, hasFocus)-> {
            if (hasFocus) openTimeBottomSheet(model.eventStartTime, view);
        });

        TextInputEditText pickEventEnd = binding.getRoot().findViewById(R.id.inputEventDatetimeEndTextInput);
        pickEventEnd.setOnFocusChangeListener((view, hasFocus)-> {
            if (hasFocus) openTimeBottomSheet(model.eventEndTime, view);
        });

        // Deadline section bindings
        TextInputEditText registrationStartEditText = binding.getRoot().findViewById(R.id.eventDeadlineRegistrationStartDate);
        registrationStartEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) openCalendarBottomSheet(model.registrationStartDate, view);
        });


        TextInputEditText registrationEndEditText = binding.getRoot().findViewById(R.id.eventDeadlineRegistrationEndDate);
        registrationEndEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) openCalendarBottomSheet(model.registrationEndDate, view);
        });


        TextInputEditText selectionStartEditText = binding.getRoot().findViewById(R.id.eventDeadlineSelectionStartDate);
        selectionStartEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) openCalendarBottomSheet(model.initialSelectionStartDate, view);
        });


        TextInputEditText selectionEndEditText = binding.getRoot().findViewById(R.id.eventDeadlineSelectionEndDate);
        selectionEndEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) openCalendarBottomSheet(model.initialSelectionEndDate, view);
        });


        TextInputEditText finalDecisionEditText = binding.getRoot().findViewById(R.id.eventDeadlineFinalDecisionDate);
        finalDecisionEditText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) openCalendarBottomSheet(model.finalAttendeeSelectionDate, view);
        });

        // Style section bindings.
        MaterialButton pickColor = binding.getRoot().findViewById(R.id.inputEventStylePickColorButton);
        pickColor.setOnClickListener(btn -> {
            ColorSheet colorSheet = new ColorSheet();
            colorSheet.show(CreateEventActivity.this, null, sheet -> {
                sheet.style(SheetStyle.BOTTOM_SHEET);
                sheet.displayToolbar(false);
                sheet.title("Color");
                sheet.onPositive(selected -> {
                    Color color = Color.valueOf(selected);
                    model.color.setValue(Optional.of(color));
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
