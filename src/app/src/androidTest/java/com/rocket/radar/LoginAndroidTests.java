package com.rocket.radar;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;



@RunWith(AndroidJUnit4.class)

public class LoginAndroidTests {

    @Test
    public void whenLoginButtonClicked_navigatesToProfile() {
        // Type into username and password fields
        onView(withId(R.id.usernameInput))
                .perform(typeText("testuser"), closeSoftKeyboard());
        onView(withId(R.id.emailInput))
                .perform(typeText("Wow@gmail.com"), closeSoftKeyboard());

    }
}