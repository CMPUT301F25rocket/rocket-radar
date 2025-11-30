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
import com.rocket.radar.events.Event;

import org.checkerframework.checker.units.qual.A;

import java.util.Date;
import java.util.Optional;

/**
 * Fragment for the Deadlines section of the event creation wizard.
 * Handles input for registration periods, selection periods, and final decision date.
 */
public class EventDeadlinesFragment extends Fragment implements InputFragment {
    private static final String TAG = EventDeadlinesFragment.class.getSimpleName();
    private ViewInputEventDeadlinesBinding binding;
    private EventDeadlinesViewModel viewModel;
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

        // Get the ViewModel scoped to the activity to preserve state across fragment replacements
        viewModel = new ViewModelProvider(requireActivity()).get(EventDeadlinesViewModel.class);

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
        Optional<Date> regStart = viewModel.registrationStartDate.getValue();
        Optional<Date> selStart = viewModel.initialSelectionStartDate.getValue();
        Optional<Date> finSelDate = viewModel.finalAttendeeSelectionDate.getValue();
        return regStart.isPresent() && selStart.isPresent() && finSelDate.isPresent()
            && regStart.get().before(selStart.get())
            && selStart.get().before(finSelDate.get());
    }

    @Override
    public Event.Builder extract(Event.Builder builder) {
        return builder
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
