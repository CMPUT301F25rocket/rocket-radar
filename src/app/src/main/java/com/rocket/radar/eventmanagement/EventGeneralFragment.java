package com.rocket.radar.eventmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

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
    private CreateEventModel model;
    // TODO: Pull all data from the model into the individual fragments.
    List<Integer> categories;

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
            categories = checkedIds;
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the shared ViewModel from the parent activity
        model = new ViewModelProvider(requireActivity()).get(CreateEventModel.class);

        // Bind the model to the view
        binding.setCreateEvent(model);
        binding.setLifecycleOwner(getViewLifecycleOwner());
    }

    @Override
    public boolean valid(InputFragment inputFragment) {
        String title = this.model.title.getValue();
        String descr = this.model.description.getValue();
        String tagline = this.model.tagline.getValue();

        return (title != null && !title.isBlank())
                && (descr != null && !descr.isBlank())
                && (tagline != null && !tagline.isBlank());
    }


    @Override
    public Event.Builder extract(Event.Builder builder) {
        for (var idx : categories) {
            builder.category(Event.allEventCategories.get(idx));
        }

        return builder.title(this.model.title.getValue())
                .description(this.model.description.getValue())
                .tagline(model.tagline.getValue());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
