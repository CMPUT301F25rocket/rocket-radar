package com.rocket.radar.eventmanagement;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rocket.radar.databinding.ViewInputEventLotteryBinding;
import com.rocket.radar.events.Event;

import java.util.Date;
import java.util.Optional;

/**
 * Fragment for the Lottery section of the event creation wizard.
 * Handles input for waitlist capacity, event capacity, location requirement, lottery date/time,
 * event date/time, and deadlines.
 */
public class EventLotteryFragment extends Fragment implements InputFragment {
    private static final String TAG = EventLotteryFragment.class.getSimpleName();
    private ViewInputEventLotteryBinding binding;
    private EventLotteryViewModel viewModel;
    private BottomSheetProvider bottomSheetProvider;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ViewInputEventLotteryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the ViewModel scoped to the activity to preserve state across fragment replacements
        viewModel = new ViewModelProvider(requireActivity()).get(EventLotteryViewModel.class);

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

        setupInputListeners();
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

    private void setupInputListeners() {
        if (bottomSheetProvider == null) {
            Log.e(TAG, "BottomSheetProvider is null, cannot set up pickers");
            return;
        }

        // Waitlist capacity input
        binding.lotterySectionWaitlistCapacityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    viewModel.waitlistCapacity.setValue(Optional.empty());
                } else {
                    try {
                        viewModel.waitlistCapacity.setValue(Optional.of(Integer.parseInt(s.toString())));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid waitlist capacity: " + s.toString());
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

        // Event capacity input
        binding.lotterySectionEventCapacityInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().isEmpty()) {
                    viewModel.eventCapacity.setValue(Optional.empty());
                } else {
                    try {
                        viewModel.eventCapacity.setValue(Optional.of(Integer.parseInt(s.toString())));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid event capacity: " + s.toString());
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });

    }

    @Override
    public boolean valid(InputFragment inputFragment) {
        Optional<Integer> waitlistCapacity = viewModel.waitlistCapacity.getValue();
        Optional<Integer> eventCapacity = viewModel.eventCapacity.getValue();
        Optional<Date> date = viewModel.eventDate.getValue();
        Optional<Time> startTime = viewModel.eventStartTime.getValue();
        Optional<Time> endTime = viewModel.eventEndTime.getValue();
        Optional<Date> regStart = viewModel.registrationStartDate.getValue();
        Optional<Date> selStart = viewModel.initialSelectionStartDate.getValue();
        Optional<Date> finSelDate = viewModel.finalAttendeeSelectionDate.getValue();

        if (viewModel.hasWaitlistCapacity.getValue() && waitlistCapacity.isEmpty())  {
            return false;
        }

        // We always need a capacity set.
        if (eventCapacity.isEmpty()) {
            return false;
        }

        // FIXME: These may not be correct. I didn't double check.
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
        // TODO: Custom Exception to store and enum for the missing field value and format it at the
        // top level.
        Date date = viewModel.eventDate.getValue()
                .orElseThrow(() -> new IllegalStateException("The event start date is missing."));

        Time startTime = viewModel.eventStartTime.getValue()
                .orElseThrow(() -> new IllegalStateException("The event start time is missing."));

        Time endTime = viewModel.eventEndTime.getValue()
                .orElseThrow(() -> new IllegalStateException("The event end time is missing."));

        return builder.waitlistCapacity(viewModel.waitlistCapacity.getValue())
                .eventCapacity(viewModel.eventCapacity.getValue().orElseThrow())
                .eventStartDate(date)
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
