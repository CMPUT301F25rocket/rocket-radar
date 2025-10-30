package com.rocket.radar;

// Add these imports for logging and handling task completion
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
// ---

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class EventRepository {

    // Add a TAG for logging, which is a best practice for debugging
    private static final String TAG = "EventRepository";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference eventsRef = database.getReference("events");

    public void createEvent(Event event) {
        // Use a completion listener to get feedback on the write operation
        Log.d(TAG, "AM I EVENT GETTING RAN BRO:??");
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "AM I EVENT GETTING RAN BRO part 3");
                eventsRef.child(event.eventTitle).setValue(event);

                Log.d(TAG, "successfully added event: " + event.getEventTitle());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "failed to add event: " + event.getEventTitle());
            }

        });
    }


    // Dummy data method remains the same...
    public List<Event> loadDummyData() {
        List<Event> eventList = new java.util.ArrayList<>();
        eventList.add(new Event("Watch Party for Oilers", "30\nSEP", "Fun for fanatics", R.drawable.rogers_image));
        eventList.add(new Event("BBQ Event", "12\nNOV", "Mushroom bros who listen to bangers", R.drawable.mushroom_in_headphones_amidst_nature));
        eventList.add(new Event("Ski Trip", "18\nDEC", "The slopes are calling", R.drawable.ski_trip_banner));
        eventList.add(new Event("Ski Trip", "18\nDEC", "The slopes are calling", R.drawable.ski_trip_banner));
        eventList.add(new Event("Ski Trip", "18\nDEC", "The slopes are calling", R.drawable.ski_trip_banner));
        eventList.add(new Event("Penultimate event", "21\nFEB", "Second to last item in list", R.drawable.mushroom_in_headphones_amidst_nature));
        eventList.add(new Event("Last event", "10\nMAR", "Last item in list", R.drawable.mushroom_in_headphones_amidst_nature));
        return eventList;
    }
    public void addDummyDatatodb(List<Event> eventList) {
        for (Event event : eventList) {
            createEvent(event);
        }
    }
}
