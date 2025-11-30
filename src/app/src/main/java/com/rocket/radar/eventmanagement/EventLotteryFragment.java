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
 * Handles input for waitlist capacity, event capacity, location requirement, and lottery date/time.
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

        // Set up input listeners
        setupInputListeners();
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

        if (viewModel.hasWaitlistCapacity.getValue() && waitlistCapacity.isEmpty())  {
            return false;
        }

        return eventCapacity.isPresent();
    }


    @Override
    public Event.Builder extract(Event.Builder builder) {
        return builder.waitlistCapacity(viewModel.waitlistCapacity.getValue())
                .eventCapacity(viewModel.eventCapacity.getValue().orElseThrow());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
