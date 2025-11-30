package com.rocket.radar.eventmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.rocket.radar.databinding.CategoryChipBinding;
import com.rocket.radar.databinding.ViewInputEventGeneralBinding;
import com.rocket.radar.events.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for the General section of the event creation wizard.
 * Handles input for event title and description.
 */
public class EventGeneralFragment extends Fragment implements InputFragment {
    private ViewInputEventGeneralBinding binding;
    private EventGeneralViewModel viewModel;
    ArrayList<String> categories;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ViewInputEventGeneralBinding.inflate(inflater, container, false);

        categories = new ArrayList<>();

        for (var category : Event.allEventCategories) {
                CategoryChipBinding chip = CategoryChipBinding.inflate(inflater, binding.createEventGeneralChipGroup.getRoot(), false);
            chip.getRoot().setText(category);
            binding.createEventGeneralChipGroup.getRoot().addView(chip.getRoot());
        }

        binding.createEventGeneralChipGroup.getRoot().setOnCheckedStateChangeListener((group, checkedIds) ->  {
            categories.clear();
            for (var viewId : checkedIds) {
                String category = ((Chip)group.findViewById(viewId)).getText().toString();
                categories.add(category);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the ViewModel scoped to the activity to preserve state across fragment replacements
        viewModel = new ViewModelProvider(requireActivity()).get(EventGeneralViewModel.class);

        // Bind the model to the view
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());
    }

    @Override
    public boolean valid(InputFragment inputFragment) {
        String title = this.viewModel.title.getValue();
        String descr = this.viewModel.description.getValue();
        String tagline = this.viewModel.tagline.getValue();

        return (title != null && !title.isBlank())
                && (descr != null && !descr.isBlank())
                && (tagline != null && !tagline.isBlank());
    }

    @Override
    public Event.Builder extract(Event.Builder builder) {
        return builder
                .categories(categories)
                .title(this.viewModel.title.getValue())
                .description(this.viewModel.description.getValue())
                .tagline(viewModel.tagline.getValue());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
