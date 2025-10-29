package com.rocket.radar;

import static android.app.PendingIntent.getActivity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class EventViewFragment {

    private final Event event;

    public EventViewFragment(Event event) {
        this.event = event;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.event_view, container, false);

        // TODO: Change toolbar to a button
        Button backButton = view.findViewById(R.id.toolbar);
//        backButton.setOnClickListener(v -> {
//            if (getActivity() != null) {
//                getActivity().getSupportFragmentManager().popBackStack();
//            }
//        });

        return view;

    }
}
