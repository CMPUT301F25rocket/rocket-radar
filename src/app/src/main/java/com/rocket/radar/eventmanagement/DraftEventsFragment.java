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

import com.rocket.radar.databinding.ActivityCreateEventBinding;
import com.rocket.radar.databinding.FragmentDraftEventsBinding;


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
            Context context = getContext();
            // Rhu roh.
            assert context != null;
            Intent createEvent = new Intent(context, CreateEventActivity.class);
            context.startActivity(createEvent);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
