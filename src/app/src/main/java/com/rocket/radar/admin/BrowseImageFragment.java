package com.rocket.radar.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.rocket.radar.databinding.FragmentBrowseImagesBinding;
import com.rocket.radar.events.EventRepository;

import java.util.ArrayList;

public class BrowseImageFragment extends Fragment {
    private FragmentBrowseImagesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBrowseImagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.fullImageList.setLayoutManager(new GridLayoutManager(getContext(), 3));
        binding.fullImageList.setAdapter(new ImageAdapter(new ArrayList<>()));
    }
}
