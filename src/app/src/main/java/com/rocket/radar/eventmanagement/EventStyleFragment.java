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

import com.rocket.radar.databinding.ViewInputEventStyleBinding;
import com.rocket.radar.events.Event;

import java.io.FileNotFoundException;
import java.util.Optional;

/**
 * Fragment for the Style section of the event creation wizard.
 * Handles input for event banner image.
 */
public class EventStyleFragment extends Fragment implements InputFragment {
    private static final String TAG = EventStyleFragment.class.getSimpleName();
    private ViewInputEventStyleBinding binding;
    private EventStyleViewModel viewModel;
    private ActivityResultLauncher<PickVisualMediaRequest> pickMedia;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Register for activity result (must be done before onCreateView)
        pickMedia = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri != null && binding != null) {
                        binding.inputEventStylePickImage.setImageURI(uri);
                        viewModel.image.setValue(Optional.of(uri));
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ViewInputEventStyleBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the ViewModel scoped to the activity to preserve state across fragment replacements
        viewModel = new ViewModelProvider(requireActivity()).get(EventStyleViewModel.class);

        // Bind the model to the view
        binding.setViewModel(viewModel);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // Set up image picker
        setupImagePicker();
    }

    private void setupImagePicker() {
        // Image picker
        binding.inputEventStylePickImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()
            );
        });
    }

    @Override
    public boolean valid(InputFragment inputFragment) {
        Optional<Uri> uri = viewModel.image.getValue();
        return uri.isPresent();
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
        return builder.bannerImage(bitmap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
