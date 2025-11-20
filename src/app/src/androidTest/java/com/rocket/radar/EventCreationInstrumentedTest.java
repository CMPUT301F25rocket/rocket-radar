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
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import org.junit.Assume;
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
        // 1. Get default instance
        db = FirebaseFirestore.getInstance();

        // 2. Point it at emulator
        db.useEmulator("10.0.2.2", 8080);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);

        // 3. Inject into EventRepository and reset singleton
        EventRepository.useFirestoreForTesting(db);

        // 4. Now this repo is guaranteed to talk to the emulator
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

    @Test
    public void testGetAllEventsStream() throws Exception {

        Tasks.await(db.collection("events").limit(1).get());

        // Now attach LiveData observers
        LiveData<List<Event>> liveData = repo.getAllEvents();
        List<Event> initial = LiveDataTestUtil.getOrAwaitValue(liveData);
        assertEquals(0, initial.size());

        // Insert event
        Event sample = EventTestUtils.sampleEvent();
        repo.createEvent(sample);
        Thread.sleep(500);

        List<Event> updated = LiveDataTestUtil.getOrAwaitValue(liveData);
        assertEquals(1, updated.size());
        assertEquals(sample.getEventTitle(), updated.get(0).getEventTitle());
    }

    @Test
    public void testAddUserToWaitlist() throws Exception {
        Event e = EventTestUtils.sampleEvent();
        repo.createEvent(e);
        Thread.sleep(300);

        GeoPoint gp = new GeoPoint(53.5, -113.5);
        repo.addUserToWaitlist(e, "user123", gp);
        Thread.sleep(300);

        DocumentSnapshot snap = Tasks.await(
                db.collection("events")
                        .document(e.getEventId())
                        .collection("waitlistedUsers")
                        .document("user123")
                        .get()
        );

        assertTrue(snap.exists());
        assertEquals(gp, snap.getGeoPoint("signupLocation"));
    }

    @Test
    public void testAddUserToWaitlist_NoLocation() throws Exception {
        Event e = EventTestUtils.sampleEvent();
        repo.createEvent(e);
        Thread.sleep(300);

        repo.addUserToWaitlist(e, "userABC", null);
        Thread.sleep(300);

        DocumentSnapshot snap = Tasks.await(
                db.collection("events")
                        .document(e.getEventId())
                        .collection("waitlistedUsers")
                        .document("userABC")
                        .get()
        );

        assertTrue(snap.exists());
        assertNull(snap.getGeoPoint("signupLocation"));
    }

}
