package com.rocket.radar.events;

/**
 * FilterEventsFragment provides a user interface for filtering the list of events.
 * Currently, it's a placeholder UI with "Confirm" and "Cancel" buttons.
 *
 * Outstanding Issues: The actual filtering logic is not yet implemented.
 */
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.rocket.radar.R;

import java.util.ArrayList;
import java.util.List;

public class FilterEventsFragment extends Fragment {

    private Button cancelButton;
    private Button confirmButton;
    private FilterModel filterModel;
    private ChipGroup chipGroup;


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
        cancelButton = view.findViewById(R.id.cancel_button);
        confirmButton = view.findViewById(R.id.confirm_button);
        chipGroup = view.findViewById(R.id.interests_chip_group);
        filterModel = new ViewModelProvider(requireActivity()).get(FilterModel.class);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) ->  {
            ArrayList<String> checkedCategories = new ArrayList<>();
            for (var chipidx : checkedIds) {
                String category = ((Chip)chipGroup.getChildAt(chipidx)).getText().toString();
                checkedCategories.add(category);
            }
            // save the categories that are selected
            // refilter all the events
            filterModel.setFilters(checkedCategories);
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
