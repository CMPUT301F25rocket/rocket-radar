package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.lifecycle.LiveData;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class EventCreationInstrumentedTest {

    private FirebaseFirestore db;
    private EventRepository repo;

    @Before
    public void setUp() {
        // Configure Firestore to use emulator
        // 10.0.2.2 is the special IP address to connect to the 'localhost' of
        // the host computer from an Android emulator.
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.useEmulator("10.0.2.2", 8080);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        firestore.setFirestoreSettings(settings);
        repo = EventRepository.getInstance();
    }

    @Test
    public void testEventCreation() throws Exception {
        EventRepository repository = EventRepository.getInstance();
        Event sample = EventTestUtils.sampleEvent();

        // This will now generate an ID if needed and set it on sample
        repository.createEvent(sample);

        // sample.getEventId() is now guaranteed non-null
        Task<DocumentSnapshot> fetchTask = FirebaseFirestore.getInstance()
                .collection("events")
                .document(sample.getEventId())
                .get();

        DocumentSnapshot snapshot = Tasks.await(fetchTask);
        Event remoteEvent = snapshot.toObject(Event.class);

        EventTestUtils.assertEventEquals(sample, remoteEvent);
    }

    @Test
    public void testCreateAndGetEvent() throws Exception {
        Event sample = EventTestUtils.sampleEvent();

        // Create the event
        repo.createEvent(sample); // fire-and-forget but OK
        Thread.sleep(500); // small buffer; since you don’t want to change createEvent()

        // Fetch using repo
        DocumentSnapshot snapshot = Tasks.await(repo.getEvent(sample.getEventId()));
        assertTrue(snapshot.exists());

        Event remote = snapshot.toObject(Event.class);
        EventTestUtils.assertEventEquals(sample, remote);
    }

}
