package com.rocket.radar;

// Add these imports for logging and handling task completion
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
// ---

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.List;

public class EventRepository {

    // Add a TAG for logging, which is a best practice for debugging
    private static final String TAG = "EventRepository";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference eventsRef = database.getReference("events");

    public void createEvent(Event event) {
        // Use a completion listener to get feedback on the write operation
        database.getReference("events").push().setValue(event)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Log a success message if the data was written
                            Log.d(TAG, "Dummy event added successfully: " + event.getEventTitle());
                        } else {
                            // Log an error message if the write failed
                            Log.e(TAG, "Failed to add dummy event.", task.getException());
                        }
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
