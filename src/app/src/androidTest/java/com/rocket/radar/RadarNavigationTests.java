package com.rocket.radar;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.widget.EditText;

import androidx.navigation.NavHostController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.rocket.radar.eventmanagement.CreateEventActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
/**
 * Instrumented tests for navigating through the Radar app.
 */
public class RadarNavigationTests {

    private static void assertCurrentFragmentIs(ActivityScenarioRule<MainActivity> rule, int expectedFragmentId) {
        rule.getScenario().onActivity(activity -> {
            NavHostFragment navHostFragment = (NavHostFragment)
                    activity.getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
            NavHostController navController = (NavHostController) navHostFragment.getNavController();

            int currentId = navController.getCurrentDestination().getId();

            // Convert IDs to readable fragment names
            String expectedName = activity.getResources().getResourceEntryName(expectedFragmentId);
            String currentName = activity.getResources().getResourceEntryName(currentId);

            assertEquals(
                    "Expected fragment <" + expectedName + "> but was <" + currentName + ">",
                    expectedFragmentId,
                    currentId
            );
        });
    }

    private static void handleSystemPermission(String buttonText) {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        UiObject allowButton = device.findObject(new UiSelector().text(buttonText));
        try {
            if (allowButton.exists()) {
                allowButton.click();
                // Give UI time to settle
                Thread.sleep(300);
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to handle system permission dialog with text: " + buttonText, e);
        }
    }

    public static void clickButtonByText(String buttonText) {
        try {
            UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            UiObject button = device.findObject(new UiSelector().text(buttonText));

            // Wait up to 3 seconds for the button to appear
            for (int i = 0; i < 30 && !button.exists(); i++) {
                Thread.sleep(100); // 100ms × 30 = 3s total
            }

            if (button.exists()) {
                button.click();
                Thread.sleep(500); // short pause for UI to update
            } else {
                throw new AssertionError("Button with text '" + buttonText + "' not found on screen.");
            }
        } catch (Exception e) {
            throw new AssertionError("Failed to click button with text: " + buttonText, e);
        }
    }

    /**
     * Types text into a visible input field.
     * You can match by hint, label text, or just the first EditText found.
     */
    public static void typeIntoField(String hintText, String input) {
        onView(allOf(
                withHint(hintText),
                isAssignableFrom(EditText.class),
                isDisplayed()
        )).perform(clearText(), typeText(input), closeSoftKeyboard());
    }

    public static void goToEventListFragment(ActivityScenarioRule<MainActivity> activityRule, int expectedFragmentId) {
        // 1. Wait for the default fragment (radarDefaultViewFragment)
        assertCurrentFragmentIs(activityRule, R.id.radarDefaultViewFragment);

        // 2. Handle the "Allow Notifications" system popup if it appears
        handleSystemPermission("Allow");

        // 3. Wait for the LoginFragment to load
        assertCurrentFragmentIs(activityRule, R.id.loginFragment);

        // 7. Start Scanning Button
        clickButtonByText("Start Scanning");

        // 8.) Allow Location Permission
        handleSystemPermission("While using the app");

        // 9.) Check that we are on the LoginStartScanningFragment
        assertCurrentFragmentIs(activityRule, R.id.loginStartScanningFragment);

        // 10.) Write in details
        typeIntoField("Name (Mandatory)", "Test User");
        typeIntoField("Email", "me@gmail.com");
        typeIntoField("Phone Number", "1234567890");
        clickButtonByText("Continue");
    }

    public static void checkValueWithId(int id, String expectedValue) {
        onView(withId(id))
                .check((view, noViewFoundException) -> {
                    if (noViewFoundException != null) {
                        throw noViewFoundException;
                    }
                    if (view instanceof EditText) {
                        EditText editText = (EditText) view;
                        String actualValue = editText.getText().toString();
                        if (!actualValue.equals(expectedValue)) {
                            throw new AssertionError("Expected value: " + expectedValue + " but was: " + actualValue);
                        }
                    } else {
                        throw new AssertionError("View with id " + id + " is not an EditText.");
                    }
                });

    }


            @Rule
            public ActivityScenarioRule<MainActivity> activityRule =
                    new ActivityScenarioRule<>(MainActivity.class);

            @Rule
            public ActivityScenarioRule<CreateEventActivity> activityRule2 =
                    new ActivityScenarioRule<>(CreateEventActivity.class);


    @Test
            public void exploreLogin() throws Exception {
                // Login Tests

                // 1. Wait for the default fragment (radarDefaultViewFragment)
                assertCurrentFragmentIs(activityRule, R.id.radarDefaultViewFragment);

                // 2. Handle the "Allow Notifications" system popup if it appears
                handleSystemPermission("Allow");

                // 3. Wait for the LoginFragment to load
                assertCurrentFragmentIs(activityRule, R.id.loginFragment);

                // 4. Criteria and Guidelines Button
                clickButtonByText("Criteria and Guidelines");

                // 5.) Check that we are on the LoginCriteriaFragment
                assertCurrentFragmentIs(activityRule, R.id.loginCriteriaFragment);

                // 6. Back to Login Screen
                onView(withId(R.id.button_back)).perform(click());

                // 7. Start Scanning Button
                clickButtonByText("Start Scanning");

                // 8.) Allow Location Permission
                handleSystemPermission("While using the app");

                // 9.) Check that we are on the LoginStartScanningFragment
                assertCurrentFragmentIs(activityRule, R.id.loginStartScanningFragment);

                // 10.) Write in details
                typeIntoField("Name (Mandatory)", "Test User");
                typeIntoField("Email", "me@gmail.com");
                typeIntoField("Phone Number", "1234567890");
                clickButtonByText("Continue");

                // 11.) Do you end up in the eventListFragment?
                assertCurrentFragmentIs(activityRule, R.id.eventListFragment);

                // 12.) Navigate to Profile Fragment
                clickButtonByText("Profile");
                assertCurrentFragmentIs(activityRule, R.id.profileFragment);

                // 13.) Account settings button
                onView(withId(R.id.account_settings_button)).perform(click());

                // 14.) Check that we are on the AccountSettingsFragment
                assertCurrentFragmentIs(activityRule, R.id.accountSettingsFragment);

                // 15.) Delete Account Button
                clickButtonByText("DELETE ACCOUNT");

                // 16.) Confirm Deletion
                clickButtonByText("Delete Account");

                // 17.) Verify we are back at the LoginFragment
                assertCurrentFragmentIs(activityRule, R.id.loginFragment);

                // 18.) Verify anonymous sign-in again
                clickButtonByText("Start Scanning");

                // 19.) Allow Location Permission again
                handleSystemPermission("While using the app");

                // 20.) Check that we are on the LoginStartScanningFragment again
                assertCurrentFragmentIs(activityRule, R.id.loginStartScanningFragment);

                // 21.) Write in details again, but wrong email and phone
                typeIntoField("Name (Mandatory)", "Test User 222222222222222222222222222222222222222222222222222222222222222222222222");
                typeIntoField("Email", "poop");
                typeIntoField("Phone Number", "0987652312312312123123123123123234321");
                clickButtonByText("Continue");

                // 22.) Verify we are still on the LoginStartScanningFragment due to validation failure
                assertCurrentFragmentIs(activityRule, R.id.loginStartScanningFragment);

                // 23.)
                // Correct the name
                typeIntoField("Name (Mandatory)", "Test User 2");
                clickButtonByText("Continue");
                assertCurrentFragmentIs(activityRule, R.id.loginStartScanningFragment);


                // Correct the email
                typeIntoField("Email", "hello@gmail.com");
                clickButtonByText("Continue");
                assertCurrentFragmentIs(activityRule, R.id.loginStartScanningFragment);


                // Correct the phone number
                typeIntoField("Phone Number", "1234567890");
                clickButtonByText("Continue");

                // 24.) Verify we are back at the EventListFragment
                assertCurrentFragmentIs(activityRule, R.id.eventListFragment);

                // Login Test Complete

                // Profile Tests
            }


    @Test
            public void exploreProfile() throws Exception {

                // Navigate to Event List Fragment first
                goToEventListFragment(activityRule, R.id.eventListFragment);

                // 1.) Navigate to Profile Fragment
                clickButtonByText("Profile");
                assertCurrentFragmentIs(activityRule, R.id.profileFragment);

                // 2.) Edit Settings Button
                onView(withId(R.id.account_settings_button)).perform(click());

                // 3.) Check that we are on the AccountSettingsFragment
                assertCurrentFragmentIs(activityRule, R.id.accountSettingsFragment);

                // 4.) Change Name and Save
                typeIntoField("Name", "Updated Test User");
                typeIntoField("Email", "new@gmail.com");
                typeIntoField("Phone Number", "5555555555");
                clickButtonByText("SAVE");

                // 5.) Verify that the name has changed
                onView(withId(R.id.account_settings_button)).perform(click());
                checkValueWithId(R.id.usernameField, "Updated Test User");
                checkValueWithId(R.id.emailField, "new@gmail.com");
                checkValueWithId(R.id.phoneField, "5555555555");
                // Profile Tests Complete
            }

            @Test
            public void exploreEvents() throws Exception {
                // Navigate to Event List Fragment first
                goToEventListFragment(activityRule, R.id.eventListFragment);

                // Event Tests

                // 1.) Create Events
                clickButtonByText("Create");
                assertCurrentFragmentIs(activityRule, R.id.draftEventsFragment);

                // 2.) Click plus to add new draft event
                onView(withId(R.id.organizing_events_create_button)).perform(click());


            }
        }
