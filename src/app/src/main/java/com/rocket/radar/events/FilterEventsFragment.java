package com.rocket.radar.events;

/**
 * FilterEventsFragment provides a user interface for filtering the list of events.
 * Currently, it's a placeholder UI with "Confirm" and "Cancel" buttons.
 *
 * Outstanding Issues: The actual filtering logic is not yet implemented.
 */
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.rocket.radar.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FilterEventsFragment extends Fragment {

    private Button cancelButton;
    private Button confirmButton;
    private CalendarView calendarView;
    private FilterModel filterModel;
    private ChipGroup chipGroup;
    private boolean dateSelected = false;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_filter_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize buttons
        calendarView = view.findViewById(R.id.calendar_view);
        cancelButton = view.findViewById(R.id.cancel_button);
        confirmButton = view.findViewById(R.id.confirm_button);
        chipGroup = view.findViewById(R.id.interests_chip_group);
        filterModel = new ViewModelProvider(requireActivity()).get(FilterModel.class);

        for (var category : filterModel.getFilters().getValue()) {
            for (int i = 0; i < chipGroup.getChildCount(); ++i) {
                View maybeChip = chipGroup.getChildAt(i);
                if (maybeChip instanceof Chip) {
                    Chip chip = (Chip)maybeChip;
                    if (chip.getText().toString().equals(category)) {
                        chip.setChecked(true);
                    }
                }
            }
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) ->  {
            ArrayList<String> checkedCategories = new ArrayList<>();
            for (var chipid : checkedIds) {
                String category = ((Chip)chipGroup.findViewById(chipid)).getText().toString();
                checkedCategories.add(category);
            }
            // save the categories that are selected
            // refilter all the events
            filterModel.setFilters(checkedCategories);
        });

        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            // Use Calendar to correctly build a Date object
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            // Optional: Set time to the beginning of the day for consistent comparisons
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // update filter model with the setDate method
            Log.d("FilterEventsFragment", "Date selected: " + calendar.getTime());
            filterModel.setDate(calendar.getTime());
        });




        // Set OnClickListener for the cancel button
        cancelButton.setOnClickListener(v -> {
            // Pop the back stack to return to the previous fragment (EventListFragment)
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        // Set OnClickListener for the confirm button
        confirmButton.setOnClickListener(v -> {
            // For now, it will also just pop the back stack.
            // TODO: Implement filter logic before popping the stack.
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }
}
