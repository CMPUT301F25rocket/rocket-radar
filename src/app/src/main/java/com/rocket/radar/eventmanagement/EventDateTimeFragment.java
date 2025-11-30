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
 * Handles input for event date, start time, and end time.
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
    }


    @Override
    public boolean valid(InputFragment inputFragment) {
        Optional<Date> date = viewModel.eventDate.getValue();
        Optional<Time> startTime = viewModel.eventStartTime.getValue();
        Optional<Time> endTime = viewModel.eventEndTime.getValue();

        return date.isPresent() && startTime.isPresent() && endTime.isPresent()
                && startTime.get().compareTo(endTime.get()) < 0;

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
                .eventEndTime(endTime);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
