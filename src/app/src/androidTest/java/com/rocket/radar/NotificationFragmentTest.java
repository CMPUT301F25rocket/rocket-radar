package com.rocket.radar;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class NotificationFragmentTest {

    private static final String PREFS_NAME = "YourAppPrefs";
    private static final String USER_ID_KEY = "USER_ID_KEY";
    private static final String TEST_USER_ID = "fragTestUser";
    private static final String TEST_EVENT_ID = "fragTestEvent";

    private FirebaseFirestore db;
    private Context targetContext;
    private TestFragmentFactory fragmentFactory; // The custom factory for injecting dependencies

    @Before
    public void setUp() {
        targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // --- APPLYING THE GUIDE'S FIX ---
        // 1. Initialize FirebaseApp if it's not already, which is common in test environments.
        if (FirebaseApp.getApps(targetContext).isEmpty()) {
            FirebaseApp.initializeApp(targetContext);
        }

        // 2. Get the Firestore instance
        db = FirebaseFirestore.getInstance();

        // 3. Disable persistence and point to the local emulator.
        //    This is the most critical part for test stability.
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build());
        // The IP "10.0.2.2" is a special alias inside the Android emulator that
        // points to the host machine's localhost (where your emulator is running).
        db.useEmulator("10.0.2.2", 8080);
        // --- END OF GUIDE'S FIX ---

        fragmentFactory = new TestFragmentFactory(db);
        loginTestUser();
        cleanupTestData(); // This will now clean the *emulator* data
    }

    @After
    public void tearDown() {
        cleanupTestData();
    }

    @Test
    public void showsEmptyState_whenUserHasNoNotifications() {
        // Use the factory to launch the fragment
        FragmentScenario.launchInContainer(NotificationFragment.class, null, fragmentFactory);
        onView(withId(R.id.empty_notifications_text)).check(matches(isDisplayed()));
        onView(withId(R.id.notifications_recycler_view)).check(matches(not(isDisplayed())));
    }

    @Test
    public void showsRecyclerView_whenUserHasNotifications() throws Exception {
        createTestNotification("My Test Notification", NotificationType.GENERIC_MESSAGE, false);
        // Use the factory to launch the fragment
        FragmentScenario.launchInContainer(NotificationFragment.class, null, fragmentFactory);
        onView(withId(R.id.notifications_recycler_view)).check(matches(isDisplayed()));
        onView(withId(R.id.empty_notifications_text)).check(matches(not(isDisplayed())));
        onView(withText("My Test Notification")).check(matches(isDisplayed()));
    }

    @Test
    public void unreadNotificationIsOpaque() throws Exception {
        createTestNotification("Unread Test", NotificationType.GENERIC_MESSAGE, false);
        // Use the factory to launch the fragment
        FragmentScenario.launchInContainer(NotificationFragment.class, null, fragmentFactory);
        onView(withText("Unread Test")).check(matches(ViewMatchers.withAlpha(1.0f)));
    }

    @Test
    public void readNotificationIsFaded() throws Exception {
        createTestNotification("Read Test", NotificationType.GENERIC_MESSAGE, true);
        // Use the factory to launch the fragment
        FragmentScenario.launchInContainer(NotificationFragment.class, null, fragmentFactory);
        onView(withText("Read Test")).check(matches(ViewMatchers.withAlpha(0.6f)));
    }

    @Test
    public void clickingGenericNotification_showsInfoDialog() throws Exception {
        createTestNotification("Generic Info", NotificationType.GENERIC_MESSAGE, false);
        // Use the factory to launch the fragment
        FragmentScenario.launchInContainer(NotificationFragment.class, null, fragmentFactory);
        onView(withId(R.id.notifications_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        onView(withId(R.id.dialog_title)).check(matches(withText("Generic Info")));
        onView(withId(R.id.dialog_message)).check(matches(isDisplayed()));
        onView(withId(R.id.button_ok)).check(matches(isDisplayed()));
    }

    @Test
    public void clickingSelectedEntrantNotification_showsRsvpDialog() throws Exception {
        createTestNotification("You're In!", NotificationType.SELECTED_ENTRANTS, false);
        // Use the factory to launch the fragment
        FragmentScenario.launchInContainer(NotificationFragment.class, null, fragmentFactory);
        onView(withId(R.id.notifications_recycler_view))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        // Note: I'm assuming the RSVP dialog's title ID is rsvp_title based on previous context.
        // Adjust if your R.id is different (e.g., lottery_title_text).
        onView(ViewMatchers.withId(R.id.rsvp_title)).check(matches(withText("You're Invited!")));
        onView(withId(R.id.button_accept)).check(matches(isDisplayed()));
        onView(withId(R.id.button_decline)).check(matches(isDisplayed()));
    }

    // --- HELPER METHODS ---

    private void loginTestUser() {
        SharedPreferences prefs = targetContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(USER_ID_KEY, TEST_USER_ID).apply();
    }

    private void createTestNotification(String title, NotificationType type, boolean isRead) throws Exception {
        // Use a repository that is guaranteed to be using our test's Firestore instance.
        NotificationRepository repository = new NotificationRepository(db);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventImageUrl", "http://example.com/image.png");
        Tasks.await(db.collection("events").document(TEST_EVENT_ID).set(eventData));

        Notification notification = new Notification(
                title,
                "Test description for " + title,
                TEST_EVENT_ID,
                type.name(),
                RecipientGroup.ALL.getValue(),
                "testSender",
                "http://example.com/image.png"
        );

        Tasks.await(repository.sendNotification(notification, Collections.singletonList(TEST_USER_ID)));

        if (isRead) {
            Tasks.await(db.collection("users").document(TEST_USER_ID).collection("notifications")
                    .whereEqualTo("title", title)
                    .limit(1).get()
                    .onSuccessTask(querySnapshot -> {
                        if (querySnapshot != null && !querySnapshot.isEmpty()) {
                            String docId = querySnapshot.getDocuments().get(0).getId();
                            return db.collection("users").document(TEST_USER_ID)
                                    .collection("notifications").document(docId)
                                    .update("read", true);
                        }
                        return Tasks.forException(new Exception("Could not find created notification to mark as read."));
                    }));
        }
    }

    private void cleanupTestData() {
        try {
            Tasks.await(db.collection("users").document(TEST_USER_ID).delete());
            Tasks.await(db.collection("events").document(TEST_EVENT_ID).delete());
        } catch (Exception e) {
            // It's okay to ignore errors during cleanup.
        }
    }
}