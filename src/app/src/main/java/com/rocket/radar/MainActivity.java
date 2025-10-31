package com.rocket.radar;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.rocket.radar.EventListFragment;
import com.rocket.radar.databinding.NavBarBinding;


public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;
    // REMOVE the repository from here. It belongs in the Fragment or a ViewModel.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navBarBinding = NavBarBinding.inflate(getLayoutInflater());
        setContentView(navBarBinding.getRoot());

        // The repository is no longer needed here
        // eventRepository = new EventRepository();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EventListFragment())
                    .commit();
        }

        navBarBinding.bottomNavigationView.setOnItemSelectedListener(item -> {
            // ... (navigation logic remains the same) ...
            return true;
        });

        // --- REMOVE THESE LINES ---
        // They don't belong here. The Fragment should manage its own data.
        // List<Event> dummyEvents = eventRepository.loadDummyData();
        // eventRepository.addDummyDatatodb(dummyEvents);
    }
}
