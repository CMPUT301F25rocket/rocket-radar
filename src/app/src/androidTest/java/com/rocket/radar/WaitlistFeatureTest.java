package com.rocket.radar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.firebase.firestore.FirebaseFirestore;
import com.rocket.radar.events.Event;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Instrumented test for testing the user waitlist feature.
 * This test interacts with the live Firestore database to ensure that adding and removing
 * a user from an event waitlist works as expected.
 */
@RunWith(AndroidJUnit4.class)
public class WaitlistFeatureTest {
    private static final String TAG = "WaitlistFeatureTest";
    private ProfileRepository profileRepository;
    private FirebaseFirestore db;
    private String testUserId;
    private String testEventId;

    @Before
    public void setup() {
        // Initialize Firestore and repositories
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        db = FirebaseFirestore.getInstance();
        profileRepository = new ProfileRepository(db);

        // Generate unique IDs for test user and event to avoid collisions
        testUserId = "testUser_" + UUID.randomUUID().toString();
        testEventId = "testEvent_" + UUID.randomUUID().toString();

        Log.d(TAG, "Setup complete. Test User ID: " + testUserId + ", Test Event ID: " + testEventId);
    }

    @Test
    public void testJoinAndLeaveWaitlist() throws InterruptedException {
        // This latch will help us wait for asynchronous Firestore operations to complete
        final CountDownLatch latch = new CountDownLatch(4); // We have 4 async operations

        // Step 1: Create a new dummy user profile in Firestore
        ProfileModel testProfile = new ProfileModel(testUserId, "Test User", "test@example.com", "1234567890", null, true, true, false);
        profileRepository.writeProfile(testProfile, new ProfileRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Step 1: Successfully created test user profile.");
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Step 1 Failed: Could not create user profile.", e);
                fail("Failed to create user profile for testing.");
            }
        });

        // Step 2: Add the test event ID to the user's waitlist
        profileRepository.addEventIdToWaitlist(testUserId, testEventId, new ProfileRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Step 2: Successfully added event to waitlist.");
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Step 2 Failed: Could not add event to waitlist.", e);
                fail("Failed to add event to waitlist.");
            }
        });

        // Wait a moment for the additions to process before reading
        Thread.sleep(1000);

        // Step 3: Read the profile back and verify the event ID is in the waitlist
        profileRepository.readProfile(testUserId, new ProfileRepository.ReadCallback() {
            @Override
            public void onProfileLoaded(ProfileModel profile) {
                assertNotNull("Profile should not be null after joining waitlist.", profile);
                assertNotNull("Waitlist should be initialized.", profile.getOnWaitlistEventIds());
                assertTrue("Event ID should be in the waitlist.", profile.getOnWaitlistEventIds().contains(testEventId));
                Log.d(TAG, "Step 3: Verification successful. Event is in waitlist.");
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Step 3 Failed: Could not read profile.", e);
                fail("Failed to read profile for verification.");
            }
        });

        // Wait again before removing
        Thread.sleep(1000);

        // Step 4: Simulate leaving the waitlist by removing the event ID
        // Note: Firestore doesn't have a direct 'remove' for a specific item like arrayUnion.
        // The standard practice is to read, modify, and write back the array.
        profileRepository.readProfile(testUserId, new ProfileRepository.ReadCallback() {
            @Override
            public void onProfileLoaded(ProfileModel profile) {
                // *** FIX: Manually set the UID on the fetched profile object ***
                profile.setUid(testUserId);

                ArrayList<String> waitlist = profile.getOnWaitlistEventIds();
                waitlist.remove(testEventId);
                profile.setOnWaitlistEventIds(waitlist);

                profileRepository.writeProfile(profile, new ProfileRepository.WriteCallback() {
                    @Override
                    public void onSuccess() {
                        // Final verification: Read one last time
                        profileRepository.readProfile(testUserId, new ProfileRepository.ReadCallback() {
                            @Override
                            public void onProfileLoaded(ProfileModel finalProfile) {
                                assertNotNull(finalProfile);
                                assertFalse("Event ID should be removed from waitlist.", finalProfile.getOnWaitlistEventIds().contains(testEventId));
                                Log.d(TAG, "Step 4: Verification successful. Event removed from waitlist.");
                                latch.countDown();
                            }

                            @Override
                            public void onError(Exception e) {
                                fail("Final verification read failed.");
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        fail("Failed to write updated profile (removing event).");
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                fail("Failed to read profile before removing event.");
            }
        });


        // Wait for all async operations to complete, with a timeout.
        boolean allOperationsCompleted = latch.await(10, TimeUnit.SECONDS);
        assertTrue("All test operations should complete within the timeout.", allOperationsCompleted);
        Log.d(TAG, "Test finished successfully.");
    }
}
