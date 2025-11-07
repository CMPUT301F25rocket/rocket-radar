package com.rocket.radar;

import com.google.firebase.Timestamp;
import com.rocket.radar.profile.ProfileModel;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for core functionalities within the ProfileModel.
 * This class verifies that user settings (like notifications and geolocation)
 * can be enabled, disabled, and default to the correct state.
 */
public class ProfileTests {

    private ProfileModel profile;

    /**
     * Sets up a mock user profile before each test case is run.
     */
    @Before
    public void setUp() {
        // Initialize with a mock user profile before each test, matching the correct constructor.
        // The isAdmin flag is added at the end.
        profile = new ProfileModel("testUser", "Test User", "test@example.com", "123-456-7890", Timestamp.now(), true, true, false);
    }

    /**
     * Verifies that a newly created ProfileModel has notifications enabled by default.
     */
    @Test
    public void notificationsAreEnabledByDefaultInNewProfile() {
        // A completely new profile should have notifications enabled by default
        ProfileModel newProfile = new ProfileModel();
        Assert.assertTrue("Notifications should be enabled by default in a new profile", newProfile.isNotificationsEnabled());
    }

    /**
     * Verifies that notifications can be successfully disabled.
     */
    @Test
    public void canDisableNotifications() {
        // Start with notifications enabled
        Assert.assertTrue("Precondition: Notifications should be enabled.", profile.isNotificationsEnabled());

        // Disable them
        profile.setNotificationsEnabled(false);

        // Verify they are now disabled
        Assert.assertFalse("It should be possible to disable notifications", profile.isNotificationsEnabled());
    }

    /**
     * Verifies that notifications can be successfully re-enabled after being disabled.
     */
    @Test
    public void canEnableNotifications() {
        // Start with notifications disabled
        profile.setNotificationsEnabled(false);
        Assert.assertFalse("Precondition: Notifications should be disabled.", profile.isNotificationsEnabled());

        // Enable them
        profile.setNotificationsEnabled(true);

        // Verify they are now enabled
        Assert.assertTrue("It should be possible to re-enable notifications", profile.isNotificationsEnabled());
    }

    /**
     * Verifies that a newly created ProfileModel has geolocation disabled by default.
     */
    @Test
    public void geolocationIsDisabledByDefaultInNewProfile() {
        ProfileModel newProfile = new ProfileModel();
        Assert.assertFalse("Geolocation should be disabled by default in a new profile", newProfile.isGeolocationEnabled());
    }

    /**
     * Verifies that geolocation can be successfully enabled.
     */
    @Test
    public void canEnableGeolocation() {
        // Start with geolocation disabled
        profile.setGeolocationEnabled(false);
        Assert.assertFalse("Precondition: Geolocation should be disabled.", profile.isGeolocationEnabled());

        // Enable it
        profile.setGeolocationEnabled(true);

        // Verify it is now enabled
        Assert.assertTrue("It should be possible to enable geolocation", profile.isGeolocationEnabled());
    }
}
