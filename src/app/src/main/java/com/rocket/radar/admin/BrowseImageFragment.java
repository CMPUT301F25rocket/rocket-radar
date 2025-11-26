package com.rocket.radar.admin;

import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.rocket.radar.R;
import com.rocket.radar.databinding.FragmentBrowseImagesBinding;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;
import com.rocket.radar.events.EventViewFragment;

import java.util.ArrayList;
import java.util.List;

public class BrowseImageFragment extends Fragment {
    private FragmentBrowseImagesBinding binding;
    public final static String TAG = BrowseImageFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBrowseImagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.fullImageList.setLayoutManager(new GridLayoutManager(getContext(), 3));
        List<Event> events = EventRepository.getInstance().getAllEvents().getValue();
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
        adapter.addOnItemClickListener(position -> {
            new MaterialAlertDialogBuilder(BrowseImageFragment.this.requireContext())
                    .setMessage("Delete the selected image")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        EventRepository.getInstance().deleteImage(events.get(position));
                    })
                    .setNegativeButton("No", (dialog, which)-> {
                        dialog.dismiss();
                    })
                    .show();
        });
        EventRepository.getInstance().getAllEvents().observe(getViewLifecycleOwner(), newEvents -> {
            images.clear();
            events.clear();
            for (var event : newEvents) {
                images.add(event.getBannerImageBitmap());
                events.add(event);
            }
            adapter.notifyDataSetChanged();
        });
    }
}
