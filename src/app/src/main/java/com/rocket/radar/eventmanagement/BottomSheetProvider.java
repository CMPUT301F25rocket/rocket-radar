package com.rocket.radar.eventmanagement;

import android.view.View;

import androidx.lifecycle.MutableLiveData;

import java.util.Date;
import java.util.Optional;

/**
 * Interface for activities that provide calendar and time picker bottom sheets.
 * This allows fragments to access these shared UI components without duplicating code.
 */
public interface BottomSheetProvider {
    /**
     * Opens a calendar bottom sheet for date selection.
     *
     * @param sink The MutableLiveData to update with the selected date
     * @param view The view that triggered the picker (for focus management)
     */
    void openCalendarBottomSheet(MutableLiveData<Optional<Date>> sink, View view);

    /**
     * Opens a time picker bottom sheet for time selection.
     *
     * @param sink The MutableLiveData to update with the selected time
     * @param view The view that triggered the picker (for focus management)
     */
    void openTimeBottomSheet(MutableLiveData<Optional<Time>> sink, View view);
}
