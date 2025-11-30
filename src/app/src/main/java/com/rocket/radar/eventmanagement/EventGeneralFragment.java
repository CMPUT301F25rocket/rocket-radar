package com.rocket.radar.eventmanagement;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.rocket.radar.databinding.CategoryChipBinding;
import com.rocket.radar.databinding.ViewInputEventGeneralBinding;
import com.rocket.radar.events.Event;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Fragment for the General section of the event creation wizard.
 * Handles input for event title, description, tagline, categories, and banner image.
 */
public class EventGeneralFragment extends Fragment implements InputFragment {
    private static final String TAG = EventGeneralFragment.class.getSimpleName();
    private ViewInputEventGeneralBinding binding;
    private EventGeneralViewModel viewModel;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;
    ArrayList<String> categories;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register for activity result (must be done before onCreateView)
        pickMedia = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null && binding != null) {
                        binding.inputEventGeneralPickImage.setImageURI(uri);
                        viewModel.image.setValue(Optional.of(uri));
                    }
                }
        );
    }

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

        // Set up image picker
        setupImagePicker();
    }

    private void setupImagePicker() {
        // Image picker
        binding.inputEventGeneralPickImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()
            );
        });
    }

    @Override
    public boolean valid(InputFragment inputFragment) {
        String title = this.viewModel.title.getValue();
        String descr = this.viewModel.description.getValue();
        String tagline = this.viewModel.tagline.getValue();
        Optional<Uri> uri = viewModel.image.getValue();

        return (title != null && !title.isBlank())
                && (descr != null && !descr.isBlank())
                && (tagline != null && !tagline.isBlank())
                && uri.isPresent();
    }

    @Override
    public Event.Builder extract(Event.Builder builder) throws Exception {
        Uri uri = viewModel.image.getValue().orElseThrow();
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
        } catch (FileNotFoundException e) {
            throw new Exception("Provided image could not be read from storage");
        }
        return builder
                .categories(categories)
                .title(this.viewModel.title.getValue())
                .description(this.viewModel.description.getValue())
                .tagline(viewModel.tagline.getValue())
                .bannerImage(bitmap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
