package com.rocket.radar.eventmanagement;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rocket.radar.databinding.ViewInputEventDeadlinesBinding;

/**
 * Fragment for the Deadlines section of the event creation wizard.
 * Handles input for registration periods, selection periods, and final decision date.
 */
public class EventDeadlinesFragment extends Fragment {
    private static final String TAG = EventDeadlinesFragment.class.getSimpleName();
    private ViewInputEventDeadlinesBinding binding;
    private CreateEventModel model;
    private BottomSheetProvider bottomSheetProvider;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ViewInputEventDeadlinesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the shared ViewModel from the parent activity
        model = new ViewModelProvider(requireActivity()).get(CreateEventModel.class);

        // Try to get the BottomSheetProvider from the parent activity
        if (requireActivity() instanceof BottomSheetProvider) {
            bottomSheetProvider = (BottomSheetProvider) requireActivity();
        } else {
            Log.e(TAG, "Parent activity does not implement BottomSheetProvider");
            return;
        }

        // Bind the model to the view
        binding.setCreateEvent(model);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // Set up date pickers
        setupDatePickers();
    }

    private void setupDatePickers() {
        if (bottomSheetProvider == null) {
            Log.e(TAG, "BottomSheetProvider is null, cannot set up pickers");
            return;
        }

        // Registration start date picker
        binding.eventDeadlineRegistrationStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(model.registrationStartDate, v);
        });

        // Registration end date picker
        binding.eventDeadlineRegistrationEndDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(model.registrationEndDate, v);
        });

        // Selection start date picker
        binding.eventDeadlineSelectionStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(model.initialSelectionStartDate, v);
        });

        // Selection end date picker
        binding.eventDeadlineSelectionEndDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(model.initialSelectionEndDate, v);
        });

        // Final decision date picker
        binding.eventDeadlineFinalDecisionDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(model.finalAttendeeSelectionDate, v);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
