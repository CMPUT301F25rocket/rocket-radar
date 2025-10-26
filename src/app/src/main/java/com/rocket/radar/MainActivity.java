package com.rocket.radar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // =====================================================================================
    // TEAM INTEGRATION GUIDE: HOW TO USE THE NOTIFICATION SYSTEM
    // =====================================================================================
    /*
     * Welcome! This guide explains how to integrate the notification system into the app.
     *
     * There are two main parts:
     *   1. The Notification Page: A full-screen list of all past notifications.
     *   2. The In-App Banner: A temporary banner that appears at the top of the screen
     *      when a new notification arrives while the user is using the app.
     *
     * To make this work, follow the steps below.
     *
     * -------------------------------------------------------------------------------------
     * STEP 1: ADD A NOTIFICATION ICON TO YOUR UI
     * -------------------------------------------------------------------------------------
     * In your main layout (e.g., in a toolbar or menu), add a notification bell icon.
     * When this icon is clicked, you will call the `navigateToNotificationFragment()` method.
     *
     * Example:
     *   ImageView notificationIcon = findViewById(R.id.your_notification_icon_id);
     *   notificationIcon.setOnClickListener(v -> navigateToNotificationFragment());
     *
     *
     * -------------------------------------------------------------------------------------
     * STEP 2: ENSURE YOUR LAYOUT HAS A FRAGMENT CONTAINER
     * -------------------------------------------------------------------------------------
     * The `navigateToNotificationFragment()` method needs a place to put the fragment.
     * Your `activity_main.xml` (or equivalent layout) must have a `FragmentContainerView`.
     *
     * Make sure this exists in your layout file:
     *   <androidx.fragment.app.FragmentContainerView
     *       android:id="@+id/main" // <--- IMPORTANT: The ID must match what's used below.
     *       android:layout_width="match_parent"
     *       android:layout_height="match_parent" />
     *
     *
     * -------------------------------------------------------------------------------------
     * STEP 3: MANAGE USER LOGIN STATE
     * -------------------------------------------------------------------------------------
     * The notification system needs to know who the current user is. This is done using
     * SharedPreferences. When a user logs IN, you must save their user ID. When they log
     * OUT, you must clear it.
     *
     * ON LOGIN:
     *   SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
     *   prefs.edit().putString(USER_ID_KEY, "the_actual_user_id_from_firebase").apply();
     *   setupInAppNotificationListener(); // IMPORTANT: Restart the listener after login.
     *
     * ON LOGOUT:
     *   SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
     *   prefs.edit().remove(USER_ID_KEY).apply();
     *   if (inAppNotificationListener != null) {
     *       inAppNotificationListener.remove(); // IMPORTANT: Stop listening on logout.
     *   }
     *
     *
     * -------------------------------------------------------------------------------------
     * STEP 4 (FOR ORGANIZERS): SENDING NOTIFICATIONS
     * -------------------------------------------------------------------------------------
     * To send a notification (e.g., after selecting lottery winners), use the
     * `NotificationController`. You do NOT need to interact with any other part of the system.
     * The controller handles everything automatically.
     *
     * Example (sending a "You're In!" message):
     *
     *   List<String> winnerIds = ... // Get the list of user IDs who won.
     *   String currentEventId = "your_event_id";
     *   String currentOrganizerId = "your_organizer_user_id";
     *
     *   NotificationController.sendNotificationToSelectedEntrants(
     *       currentEventId,
     *       currentOrganizerId,
     *       winnerIds
     *   ).addOnSuccessListener(aVoid -> {
     *       // Notification sent successfully!
     *   }).addOnFailureListener(e -> {
     *       // Something went wrong.
     *   });
     *
     */

    // --- Configuration Constants ---
    // Use these constants to avoid "magic strings".
    private static final String USER_ID_KEY = "USER_ID_KEY";
    private static final String PREFS_NAME = "YourAppPrefs";

    // This holds the real-time listener so we can detach it later to prevent memory leaks.
    private ListenerRegistration inAppNotificationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // This method starts the listener that shows the top banner for new notifications.
        setupInAppNotificationListener();

        // These buttons are for testing the system. You can remove this for production.
        setupTestButtons();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // IMPORTANT: Clean up the listener when the activity is destroyed to prevent memory leaks.
        if (inAppNotificationListener != null) {
            inAppNotificationListener.remove();
            Log.d(TAG, "In-app notification listener removed.");
        }
    }

    /**
     * Sets up a real-time Firestore listener that watches for NEW notifications
     * for the currently "logged-in" user and triggers the in-app banner.
     */
    private void setupInAppNotificationListener() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentUserId = prefs.getString(USER_ID_KEY, null);

        // If no user is logged in, there's nothing to listen to.
        if (currentUserId == null) {
            Log.d(TAG, "No user logged in. Skipping in-app listener setup.");
            return;
        }

        // If a listener already exists, remove it before creating a new one.
        // This is important for handling user login/logout without creating duplicate listeners.
        if (inAppNotificationListener != null) {
            inAppNotificationListener.remove();
        }

        Log.d(TAG, "Setting up in-app banner listener for user: " + currentUserId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        inAppNotificationListener = db.collection("users").document(currentUserId).collection("notifications")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen error", error);
                        return;
                    }

                    // Loop through the CHANGES in the collection, not the whole collection.
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        // We only care about newly added notifications to show the banner.
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            Notification notification = dc.getDocument().toObject(Notification.class);
                            Log.d(TAG, "New notification received via listener: " + notification.getTitle());

                            // TODO: Add logic here to check if the user has opted-in to notifications.
                            // For example:
                            // boolean hasOptedIn = UserPreferences.hasOptedInForBanners(this);
                            // if (hasOptedIn) {
                            showTopBanner(notification);
                            // }
                        }
                    }
                });
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
     * Sets up temporary buttons for testing purposes.
     * TODO: Add these buttons to your activity_main.xml to use them.
     *
     *   <Button
     *       android:id="@+id/button_fake_login"
     *       android:text="Fake Login (testUser123)" ... />
     *   <Button
     *       android:id="@+id/button_send_test_notif"
     *       android:text="Send Test Notification" ... />
     *   <Button
     *       android:id="@+id/button_open_notif_page"
     *       android:text="Open Notification Page" ... />
     */
    private void setupTestButtons() {
        // NOTE: These IDs are placeholders. You must add the buttons to your layout to use them.
        Button fakeLoginButton = findViewById(R.id.button_fake_login);
        Button sendNotifButton = findViewById(R.id.button_send_test_notif);
        Button openNotifPageButton = findViewById(R.id.button_open_notif_page);

        if (fakeLoginButton == null || sendNotifButton == null || openNotifPageButton == null) {
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

            NotificationController.sendNotificationToSelectedEntrants(eventId, organizerId, recipients)
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
    }
}
