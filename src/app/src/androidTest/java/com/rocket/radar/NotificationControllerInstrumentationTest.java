package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RunWith(AndroidJUnit4.class)
public class NotificationControllerInstrumentationTest {

    private static final String TEST_USER_ID = "integrationTestUser";
    private static final String TEST_EVENT_ID = "integrationTestEvent";
    private static final String TEST_SENDER_ID = "integrationTestSender";
    private static final String TEST_IMAGE_URL = "http://example.com/test_image.png";
    private static final String TAG = "NotificationControllerTest";

    private FirebaseFirestore db;
    // We now also have a repository instance for the test
    private NotificationRepository repository;

    @Before
    public void setUp() {
        db = FirebaseFirestore.getInstance();
        // Initialize the repository with the single Firestore instance
        repository = new NotificationRepository(db);
        cleanupTestData();
    }

    @After
    public void tearDown() {
        cleanupTestData();
    }

    @Test
    public void testSendNotificationToSelectedEntrants_HappyPath() throws ExecutionException, InterruptedException {
        // 1. ARRANGE
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventImageUrl", TEST_IMAGE_URL);
        Tasks.await(db.collection("events").document(TEST_EVENT_ID).set(eventData));

        // *** THIS IS THE FIX ***
        // Instead of new NotificationController(), we pass in the test's 'db' and 'repository'.
        // This ensures only ONE Firestore instance is used.
        NotificationController controller = new NotificationController(db, repository);
        List<String> recipients = new ArrayList<>();
        recipients.add(TEST_USER_ID);

        // 2. ACT
        Task<Void> sendTask = controller.sendNotificationToSelectedEntrants(
                TEST_EVENT_ID,
                TEST_SENDER_ID,
                recipients
        );
        Tasks.await(sendTask);

        assertTrue("The sendNotification task should be successful.", sendTask.isSuccessful());

        // 3. ASSERT
        Task<QuerySnapshot> getTask = db.collection("users").document(TEST_USER_ID).collection("notifications").get();
        QuerySnapshot snapshot = Tasks.await(getTask);

        assertEquals("There should be exactly one notification for the test user.", 1, snapshot.size());
        Notification createdNotification = snapshot.getDocuments().get(0).toObject(Notification.class);
        assertNotNull(createdNotification);

        assertEquals("You're In!", createdNotification.getTitle());
        assertEquals(TEST_IMAGE_URL, createdNotification.getImageUrl());
    }

    @Test
    public void testSendNotification_Fails_ForInvalidEventId() {
        // ARRANGE
        // *** THIS IS THE FIX ***
        // Use the dependency injection constructor here as well.
        NotificationController controller = new NotificationController(db, repository);
        List<String> recipients = new ArrayList<>();
        recipients.add("someUser");

        // ACT
        Task<Void> sendTask = controller.sendNotificationToSelectedEntrants(
                "THIS_EVENT_ID_DOES_NOT_EXIST",
                "someSender",
                recipients
        );

        // ASSERT
        try {
            Tasks.await(sendTask);
            fail("The task should have failed for an invalid event ID, but it succeeded.");
        } catch (ExecutionException e) {
            assertNotNull("The cause of the failure should not be null.", e.getCause());
            assertTrue("The error message should indicate that the document was not found.",
                    e.getCause().getMessage().contains("Event document not found"));
            Log.d(TAG, "Successfully caught expected failure: " + e.getCause().getMessage());
        } catch (InterruptedException e) {
            fail("Test was interrupted: " + e.getMessage());
        }
    }

    private void cleanupTestData() {
        try {
            Tasks.await(db.collection("users").document(TEST_USER_ID).delete());
            Tasks.await(db.collection("events").document(TEST_EVENT_ID).delete());
            Log.d(TAG, "Cleanup successful.");
        } catch (Exception e) {
            Log.w(TAG, "Ignoring error during cleanup: " + e.getMessage());
        }
    }
}
