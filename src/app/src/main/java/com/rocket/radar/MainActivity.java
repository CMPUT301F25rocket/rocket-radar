package com.rocket.radar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity of the application.
 * This class includes a full guide and testing suite for the notification system.
 * Author: Braden Woods
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // --- Configuration Constants ---
    private static final String USER_ID_KEY = "USER_ID_KEY";
    private static final String PREFS_NAME = "YourAppPrefs";

    // This holds the real-time listener so we can detach it later to prevent memory leaks.
    private ListenerRegistration inAppNotificationListener;

    // The controller is now an instance field.
    private NotificationController notificationController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the controller once when the activity is created.
        notificationController = new NotificationController();

        // Standard boilerplate for handling system screen insets.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new EventListFragment())
                    .commit();
        }
        // This method starts the listener that shows the top banner for new notifications.
        setupInAppNotificationListener();

        // These buttons are for testing the system. You can remove this for production.
        setupTestButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // It's good practice to re-setup the listener in onResume in case the user
        // logs in or out in another part of the app and then returns here.
        setupInAppNotificationListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Clean up the listener when the activity is not visible to save resources.
        if (inAppNotificationListener != null) {
            inAppNotificationListener.remove();
            inAppNotificationListener = null; // Set to null to indicate it's detached.
            Log.d(TAG, "In-app notification listener removed on pause.");
        }
    }

    /**
     * Sets up a real-time Firestore listener that watches for NEW notifications
     * for the currently "logged-in" user and triggers the in-app banner.
     */
    private void setupInAppNotificationListener() {
        // If a listener already exists, don't create a new one.
        if (inAppNotificationListener != null) {
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUserId = prefs.getString(USER_ID_KEY, null);

        // If no user is logged in, there's nothing to listen to.
        if (currentUserId == null) {
            Log.d(TAG, "No user logged in. Skipping in-app listener setup.");
            return;
        }

        Log.d(TAG, "Setting up in-app banner listener for user: " + currentUserId);
        try {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            inAppNotificationListener = db.collection("users").document(currentUserId).collection("notifications")
                    .addSnapshotListener((snapshots, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Listen error", error);
                            return;
                        }

                        if (snapshots == null) {
                            return;
                        }

                        // Loop through the CHANGES in the collection, not the whole collection.
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            // We only care about newly added notifications to show the banner.
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                Notification notification = dc.getDocument().toObject(Notification.class);
                                if (notification.getTitle() != null) {
                                    Log.d(TAG, "New notification received via listener: " + notification.getTitle());
                                    showTopBanner(notification);
                                }
                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e(TAG, "Firebase not initialized. Make sure google-services.json is correct and you have a custom Application class.", e);
            Toast.makeText(this, "CRITICAL: Firebase is not initialized.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Displays the custom in-app notification banner at the top of the screen.
     * @param notification The notification to display.
     */
    private void showTopBanner(Notification notification) {
        // Find the root view of the activity to anchor the banner.
        View anchorView = findViewById(android.R.id.content);
        if (anchorView == null) return;

        InAppNotifier.showNotification(
                anchorView,
                notification.getTitle(),
                notification.getDescription(),
                v -> {
                    // This is the click listener for the banner.
                    // When tapped, it will open the NotificationFragment.
                    Log.d(TAG, "In-app banner clicked. Navigating to notifications page.");
                    navigateToNotificationFragment();
                }
        );
    }

    /**
     * Handles the navigation to show the NotificationFragment.
     * This is the method you should call from your notification bell icon's click listener.
     */
    private void navigateToNotificationFragment() {
        NotificationFragment fragment = new NotificationFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragment) // Make sure R.id.main is your FragmentContainerView ID.
                .addToBackStack(null) // Allows user to press the back button to return.
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }

    /**
     * Handles the navigation to show the EventListFragment.
     */
    private void navigateToEventListFragment() {
        EventListFragment fragment = new EventListFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main, fragment) // Replaces content in the FragmentContainerView
                .addToBackStack(null) // Allows user to press back to return
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .commit();
    }


    /**
     * Sets up temporary buttons for testing purposes.
     */
    private void setupTestButtons() {
        Button fakeLoginButton = findViewById(R.id.button_fake_login);
        Button sendNotifButton = findViewById(R.id.button_send_test_notif);
        Button openNotifPageButton = findViewById(R.id.button_open_notif_page);
        Button eventListButton = findViewById(R.id.button_eventList); // Find the new button

        if (fakeLoginButton == null || sendNotifButton == null || openNotifPageButton == null || eventListButton == null) {
            Log.w(TAG, "Test buttons not found in layout. Skipping setup. Add them to activity_main.xml to enable testing.");
            return;
        }

        // FAKE LOGIN: Simulates a user logging in and starts the listener.
        fakeLoginButton.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(USER_ID_KEY, "testUser123").apply();
            Toast.makeText(this, "Logged in as 'testUser123'", Toast.LENGTH_SHORT).show();
            // After logging in, we must restart the listener.
            setupInAppNotificationListener();
        });

        // SEND NOTIFICATION: Uses the controller to send a notification to our test user.
        sendNotifButton.setOnClickListener(v -> {
            List<String> recipients = new ArrayList<>();
            recipients.add("testUser123");
            String eventId = "event_abc_789"; // A test event ID.
            String organizerId = "organizer_xyz_123";

            // THIS IS THE FIX: Call the method on the instance, not the class.
            notificationController.sendNotificationToSelectedEntrants(eventId, organizerId, recipients)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Test notification sent!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to send notification.", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to send test notification", task.getException());
                        }
                    });
        });

        // OPEN PAGE: Manually opens the notification list page.
        openNotifPageButton.setOnClickListener(v -> navigateToNotificationFragment());

        // OPEN EVENT LIST PAGE: Manually opens the event list page.
        eventListButton.setOnClickListener(v -> navigateToEventListFragment());
    }
}
