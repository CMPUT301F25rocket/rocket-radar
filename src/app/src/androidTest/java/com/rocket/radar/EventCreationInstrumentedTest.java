package com.rocket.radar;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EventCreationInstrumentedTest {

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
    }

    @Test
    public void testEventCreation()
    throws Exception
    {
        EventRepository repository = EventRepository.getInstance();
        Event sample = EventTestUtils.sampleEvent();
        repository.createEvent(sample);
        Event remoteEvent = FirebaseFirestore.getInstance()
                .collection("events")
                .document(sample.getEventId())
                .get()
                .getResult()
                .toObject(Event.class);
        EventTestUtils.assertEventEquals(sample, remoteEvent);
    }
}
