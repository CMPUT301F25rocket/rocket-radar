package com.rocket.radar.eventmanagement;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.maxkeppeler.sheets.color.ColorSheet;
import com.maxkeppeler.sheets.core.SheetStyle;
import com.rocket.radar.R;
import com.rocket.radar.databinding.ViewInputEventStyleBinding;
import com.rocket.radar.events.Event;

import java.io.FileNotFoundException;
import java.util.Optional;

import kotlin.Unit;

/**
 * Fragment for the Style section of the event creation wizard.
 * Handles input for event banner image and color theme.
 */
public class EventStyleFragment extends Fragment implements InputFragment {
    private static final String TAG = EventStyleFragment.class.getSimpleName();
    private ViewInputEventStyleBinding binding;
    private CreateEventModel model;
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
                        model.image.setValue(Optional.of(uri));
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

        // Get the shared ViewModel from the parent activity
        model = new ViewModelProvider(requireActivity()).get(CreateEventModel.class);

        // Bind the model to the view
        binding.setCreateEvent(model);
        binding.setLifecycleOwner(getViewLifecycleOwner());

        // Set up style pickers
        setupStylePickers();
    }

    private String colorToString(Integer color) {
        if (color == ContextCompat.getColor(requireContext(), R.color.red))
            return "Red";

        if (color == ContextCompat.getColor(requireContext(), R.color.orange))
            return "Orange";

        if (color == ContextCompat.getColor(requireContext(), R.color.yellow))
            return "Yellow";

        if (color == ContextCompat.getColor(requireContext(), R.color.green))
            return "Green";

        if (color == ContextCompat.getColor(requireContext(), R.color.cyan))
            return "Cyan";

        if (color == ContextCompat.getColor(requireContext(), R.color.blue))
            return "Blue";

        return color.toString();
    }

    private void setupStylePickers() {
        // Color picker
        binding.inputEventStylePickColorButton.setOnClickListener(btn -> {
            ColorSheet colorSheet = new ColorSheet();
            colorSheet.show(requireActivity(), null, sheet -> {
                sheet.style(SheetStyle.BOTTOM_SHEET);
                sheet.disableAlpha();
                sheet.colorsRes(R.color.red, R.color.orange, R.color.yellow, R.color.green, R.color.cyan, R.color.blue);
                sheet.onPositive(selected -> {
                    Color color = Color.valueOf(selected);
                    model.color.setValue(Optional.of(color));
                    binding.inputEventStylePickColorButton.setBackgroundColor(color.toArgb());
                    binding.inputEventStylePickColorButton.setText(colorToString(selected));
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
        });

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
        Optional<Color> color = model.color.getValue();
        Optional<Uri> uri = model.image.getValue();
        return color.isPresent() && uri.isPresent();
    }

    @Override
    public Event.Builder extract(Event.Builder builder) throws Exception {
        Uri uri = model.image.getValue().orElseThrow();
        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
        } catch (FileNotFoundException e) {
            throw new Exception("Provided image could not be read from storage");
        }
        return builder.bannerImage(bitmap)
                .color(model.color.getValue().orElseThrow());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
