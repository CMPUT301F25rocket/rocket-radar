package com.rocket.radar.admin;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rocket.radar.R;
import com.rocket.radar.databinding.FragmentBrowseImagesBinding;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.events.EventViewFragment;

import java.util.ArrayList;
import java.util.List;

public class BrowseImageFragment extends Fragment implements DialogInterface.OnClickListener {
    private FragmentBrowseImagesBinding binding;
    public final static String TAG = BrowseImageFragment.class.getSimpleName();
    private List<Event> events;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBrowseImagesBinding.inflate(inflater, container, false);
        events = EventRepository.getInstance().getAllEvents().getValue();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.fullImageList.setLayoutManager(new GridLayoutManager(getContext(), 3));
        ArrayList<Bitmap> images = new ArrayList<>();
        if (events == null) {
            Log.w(TAG, "Failed to fetch Events from EventRepository");
        } else {
            for (var event : events) {
                images.add(event.getBannerImageBitmap());
            }
        }
        ImageAdapter adapter = new ImageAdapter(images);
        binding.fullImageList.setAdapter(adapter);
        adapter.addOnItemClickListener(position -> {
            EventViewFragment eventViewFragment = EventViewFragment.newInstance(events.get(position));
            BrowseImageFragment.this.requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, eventViewFragment)
                    .addToBackStack(null)
                    .commit();
        });
        adapter.addOnItemLongClickListener(this::startDeleteDialog);
        EventRepository.getInstance().getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            images.clear();
            BrowseImageFragment.this.events = newEvents;
            for (var event : newEvents) {
                images.add(event.getBannerImageBitmap());
            }
            adapter.notifyDataSetChanged();
        });
    }

    private Event dialogContextEvent = null;
    public void startDeleteDialog(int position) {
        dialogContextEvent = events.get(position);
        new MaterialAlertDialogBuilder(this.requireContext())
                .setMessage("Delete the selected image")
                .setPositiveButton("Yes", this)
                .setNegativeButton("No", this)
                .show();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            EventRepository.getInstance()
                .deleteImage(
                    dialogContextEvent,
                    val -> Toast.makeText(requireActivity(), "Deleted Image", Toast.LENGTH_SHORT).show(),
                    why -> Toast.makeText(requireActivity(), "Failed ot delete", Toast.LENGTH_SHORT).show()
                );
        }
        dialog.dismiss();
    }
}
