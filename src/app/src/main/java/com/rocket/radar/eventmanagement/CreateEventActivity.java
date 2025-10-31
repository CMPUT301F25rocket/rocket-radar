package com.rocket.radar.eventmanagement;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
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

        // FIXME: Need to figure out how to notify view that section was changed.
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
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
