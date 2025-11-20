package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import androidx.lifecycle.LiveData;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.rocket.radar.notifications.Notification;
import com.rocket.radar.notifications.NotificationRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class NotificationInstrumentedTest {

    private FirebaseFirestore db;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        db.useEmulator("10.0.2.2", 8080);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build();
        db.setFirestoreSettings(settings);
    }

    @Test
    public void testGetMyNotifications_resolvesFromTopLevel() throws Exception {
        String testUserId = "testUser123";

        // 1. Seed a top-level notification document
        Map<String, Object> content = new HashMap<>();
        content.put("eventTitle", "Test Title");
        content.put("notificationType", "Test Body");
        content.put("image", R.drawable.ic_radar);
        content.put("timestamp", FieldValue.serverTimestamp());

        DocumentReference contentRef = Tasks.await(
                db.collection("notifications").add(content)
        );

        // 2. Seed a user stub in users/{uid}/notifications
        DocumentReference stubRef = db.collection("users").document(testUserId)
                .collection("notifications").document();

        Map<String, Object> stub = new HashMap<>();
        stub.put("readStatus", false);
        stub.put("notificationRef", contentRef);

        Tasks.await(stubRef.set(stub));

        // 3. Use the test-friendly constructor
        NotificationRepository repo = new NotificationRepository(testUserId);

        LiveData<List<Notification>> liveData = repo.getMyNotifications();

        // 4. Wait for LiveData to emit (using CountDownLatch helper)
        List<Notification> result = LiveDataTestUtil.getOrAwaitValue(liveData);

        // 5. Assert
        assertFalse(result.isEmpty());
        Notification notif = result.get(0);
        assertEquals("Test Title", notif.getEventTitle());
        assertEquals("Test Body", notif.getNotificationType());
        assertEquals(false, notif.isReadStatus());
    }
}

