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

import com.rocket.radar.databinding.ViewInputEventDatetimeBinding;
import com.rocket.radar.events.Event;

import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Fragment for the Date & Time section of the event creation wizard.
 * Handles input for event date, start time, end time, and all deadline dates.
 */
public class EventDateTimeFragment extends Fragment implements InputFragment {
    private static final String TAG = EventDateTimeFragment.class.getSimpleName();
    private ViewInputEventDatetimeBinding binding;
    private EventDateTimeViewModel viewModel;
    private BottomSheetProvider bottomSheetProvider;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ViewInputEventDatetimeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the ViewModel scoped to the activity to preserve state across fragment replacements
        viewModel = new ViewModelProvider(requireActivity()).get(EventDateTimeViewModel.class);

        // Try to get the BottomSheetProvider from the parent activity
        if (requireActivity() instanceof BottomSheetProvider) {
            bottomSheetProvider = (BottomSheetProvider) requireActivity();
        } else {
            Log.e(TAG, "Parent activity does not implement BottomSheetProvider");
            return;
        }

        // Bind the model to the view
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // Set up date and time pickers
        setupDateTimePickers();
    }

    private void setupDateTimePickers() {
        if (bottomSheetProvider == null) {
            Log.e(TAG, "BottomSheetProvider is null, cannot set up pickers");
            return;
        }

        // Event date picker
        binding.inputEventDatetimeDaterangeTextInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(viewModel.eventDate, v);
        });

        // Event start time picker
        binding.inputEventDatetimeStartTextInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openTimeBottomSheet(viewModel.eventStartTime, v);
        });

        // Event end time picker
        binding.inputEventDatetimeEndTextInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openTimeBottomSheet(viewModel.eventEndTime, v);
        });

        // Registration start date picker
        binding.eventDeadlineRegistrationStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(viewModel.registrationStartDate, v);
        });

        // Selection start date picker
        binding.eventDeadlineSelectionStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(viewModel.initialSelectionStartDate, v);
        });

        // Final decision date picker
        binding.eventDeadlineFinalDecisionDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) bottomSheetProvider.openCalendarBottomSheet(viewModel.finalAttendeeSelectionDate, v);
        });
    }


    @Override
    public boolean valid(InputFragment inputFragment) {
        Optional<Date> date = viewModel.eventDate.getValue();
        Optional<Time> startTime = viewModel.eventStartTime.getValue();
        Optional<Time> endTime = viewModel.eventEndTime.getValue();
        Optional<Date> regStart = viewModel.registrationStartDate.getValue();
        Optional<Date> selStart = viewModel.initialSelectionStartDate.getValue();
        Optional<Date> finSelDate = viewModel.finalAttendeeSelectionDate.getValue();

        // Validate event date and times
        boolean eventDateTimeValid = date.isPresent() && startTime.isPresent() && endTime.isPresent()
                && startTime.get().compareTo(endTime.get()) < 0;

        // Validate deadlines
        boolean deadlinesValid = regStart.isPresent() && selStart.isPresent() && finSelDate.isPresent()
                && regStart.get().before(selStart.get())
                && selStart.get().before(finSelDate.get());

        return eventDateTimeValid && deadlinesValid;
    }

    @Override
    public Event.Builder extract(Event.Builder builder) {
        Date date = viewModel.eventDate.getValue()
                .orElseThrow(() -> new NoSuchElementException("The event start date is missing."));

        Time startTime = viewModel.eventStartTime.getValue()
                .orElseThrow(() -> new NoSuchElementException("The event start time is missing."));

        Time endTime = viewModel.eventEndTime.getValue()
                .orElseThrow(() -> new NoSuchElementException("The event end time is missing."));

        return builder.eventStartDate(date)
                .eventStartTime(startTime)
                .eventEndTime(endTime)
                .registrationStartDate(viewModel.registrationStartDate.getValue().orElseThrow())
                .initialSelectionStartDate(viewModel.initialSelectionStartDate.getValue().orElseThrow())
                .finalSelectionDate(viewModel.finalAttendeeSelectionDate.getValue().orElseThrow());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
