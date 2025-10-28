package com.rocket.radar;

import android.view.View;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * A custom Hamcrest Matcher for Espresso to check the alpha property of a View.
 * This allows UI tests to verify visual states like opacity, which is not possible
 * with Espresso's built-in matchers. It is used to confirm that read notifications
 * are visually distinct from unread ones.
 */
public class AlphaMatcher {

    /**
     * Creates a matcher that matches a view if its alpha property is equal to the given value.
     * @param expectedAlpha The expected alpha value (e.g., 1.0f for opaque, 0.6f for faded).
     * @return A Matcher for use with Espresso's onView().check().
     */
    public static Matcher<View> withAlpha(final float expectedAlpha) {
        return new TypeSafeMatcher<View>() {
            /**
             * The core logic of the matcher. This is where the actual check happens.
             * @param view The view that Espresso has found.
             * @return true if the view's alpha matches the expected alpha, false otherwise.
             */
            @Override
            protected boolean matchesSafely(View view) {
                // Compare the view's actual alpha with the expected alpha.
                // We use a small tolerance (epsilon) because floating-point numbers
                // can have precision issues.
                return Math.abs(view.getAlpha() - expectedAlpha) < 0.001;
            }

            /**
             * Describes the matcher for clear error messages.
             * If the test fails, this text will be part of the error log, making it
             * easy to understand what the test was trying to check.
             * Example: "Expected: with alpha value of <0.6F>"
             * @param description The description object to append the text to.
             */
            @Override
            public void describeTo(Description description) {
                description.appendText("with alpha value of ").appendValue(expectedAlpha);
            }
        };
    }
}
