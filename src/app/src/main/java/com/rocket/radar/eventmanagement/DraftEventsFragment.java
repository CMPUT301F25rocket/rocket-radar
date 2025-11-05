package com.rocket.radar.eventmanagement;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.BindingAdapter;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.rocket.radar.MainActivity;
import com.rocket.radar.databinding.ActivityCreateEventBinding;
import com.rocket.radar.databinding.FragmentDraftEventsBinding;
import com.rocket.radar.profile.ProfileViewModel;


public class DraftEventsFragment extends Fragment {
    FragmentDraftEventsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDraftEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.organizingEventsCreateButton.setOnClickListener(button -> {
            // Rhu roh.
            Intent createEvent = new Intent(getActivity(), CreateEventActivity.class);
            startActivity(createEvent);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
