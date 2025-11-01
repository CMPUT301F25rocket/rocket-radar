package com.rocket.radar.eventmanagement;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.textfield.TextInputEditText;
import com.rocket.radar.R;
import com.rocket.radar.databinding.ActivityCreateEventBinding;

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
    BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // These three lines took way too long to write. ʕノ•ᴥ•ʔノ ︵ ┻━┻
        // WARN: Make sure when you create variables you call setMyVarName(...) on the binding.
        binding = ActivityCreateEventBinding.inflate(getLayoutInflater());
        model = new CreateEventModel();
        binding.setCreateEvent(model);

        // >:? This needs to be set in code.
        bottomSheetBehavior = BottomSheetBehavior.from(binding.createEventWizardBottomSheet);

        // Main navigation buttons
        binding.createEventWizardNavLeftButton.setOnClickListener(btn -> {
            model.prevSection();
        });
        binding.createEventWizardNavRightButton.setOnClickListener(btn -> {
            model.nextSection();
        });

        // Unfortunately it seems viewbinding fails for included views.
        setContentView(binding.getRoot());
        binding.setLifecycleOwner(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // Now we need to bind the inputs that function by sheet.
        TextInputEditText pickEventDate = binding.getRoot().findViewById(R.id.inputEventDatetimeDaterangeTextInput);
        TextInputEditText pickEventStart = binding.getRoot().findViewById(R.id.inputEventDatetimeStartTextInput);
        TextInputEditText pickEventEnd = binding.getRoot().findViewById(R.id.inputEventDatetimeEndTextInput);

        pickEventDate.setOnClickListener(itxt -> {

        });

        pickEventStart.setOnClickListener(itxt -> {
            // TODO: Tommorow.
            // Ideally need to be able to specify the destination to save the picked value in.
            // Probably want a callback that is run whenever the datetime date is selected, time
            // is set, or color is chosen. Something like DateSheetPicker.onSelect(value -> model.eventDate = value);
            // That means I need dedicated classes for each picker type I want to implement so thats
            // going to be a pain.

            // SheetPicker<T>. Thats a better abstraction. Set the type T, and provide an instance
            // which implements a picker interface that can inflate the relevant picker into the
            // bottom sheet. Accept a callback to set value T when a selection happens. etc.
            // I probably want a single sheetpicker per xml view that has a bottom sheet which luckily
            // for this activity is just one. Then the SheetPicker<T> can own the BottomSheetBehaviour
            // And deal with the annoying stuff for me.
        });

        pickEventEnd.setOnClickListener(itxt -> {

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
