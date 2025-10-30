package com.rocket.radar;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.rocket.radar.EventRepository;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rocket.radar.databinding.NavBarBinding;
import java.util.List; // Import the List interface

public class MainActivity extends AppCompatActivity {
    private NavBarBinding navBarBinding;
    private EventRepository eventRepository; // Declare an EventRepository instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navBarBinding = NavBarBinding.inflate(getLayoutInflater());
        setContentView(navBarBinding.getRoot());

        // Initialize the repository
        eventRepository = new EventRepository();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EventListFragment())
                    .commit();
        }

        navBarBinding.bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if (item.getItemId() == R.id.action_events) {
                selectedFragment = new EventListFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // --- This is the corrected way to call your methods ---
        // 1. Load the data using the repository instance
        List<Event> dummyEvents = eventRepository.loadDummyData();
        // 2. Add the data to the database using the repository instance
        eventRepository.addDummyDatatodb(dummyEvents);
    }
}
