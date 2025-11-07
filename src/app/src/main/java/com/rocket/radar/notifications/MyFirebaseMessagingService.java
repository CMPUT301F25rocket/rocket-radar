package com.rocket.radar.notifications; // Corrected package

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.rocket.radar.R;

/**
 * Handles Firebase Cloud Messaging (FCM) services for receiving push notifications.
 * This service is responsible for processing incoming messages from FCM, displaying
 * system notifications, and handling FCM token refreshes.
 *
 * <p><b>Outstanding Issues:</b></p>
 * This file is considered deprecated and is not currently in use because the project
 * does not have access to a paid Firebase plan required for deploying Cloud Functions.
 * Without Cloud Functions, we cannot programmatically send push notifications from the
 * server-side logic (e.g., when a new event announcement is made). It is being kept
 * in the codebase in case this functionality becomes available in the future.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFCMService";
    private static final String CHANNEL_ID = "event_notifications_channel";


    /**
     * Called when a new FCM message is received from the server.
     * This method processes the incoming message, checks user preferences,
     * and triggers a system notification if appropriate.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM Message Received! From: " + remoteMessage.getFrom());

        // This logic for user preferences is fine to keep.
        SharedPreferences prefs = getSharedPreferences("AppUserSettings", Context.MODE_PRIVATE);
        boolean areNotificationsEnabled = prefs.getBoolean("notificationsEnabled", true);

        if (!areNotificationsEnabled) {
            Log.d(TAG, "User has disabled notifications. Message ignored.");
            return;
        }

        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();

            Log.d(TAG, "Notification Title: " + title);
            Log.d(TAG, "Notification Body: " + body);

            // 1. Display a system tray notification to the user.
            sendSystemNotification(title, body);

            // 2. Add the notification to the in-app list for ALL users.
            //addNotificationToInAppList(title, body);
        }
    }

    /**
     * Creates and displays a system notification in the device's notification tray.
     * It also handles the creation of a notification channel for Android 8.0 (Oreo) and above.
     *
     * @param title The title of the notification.
     * @param body The main content/body of the notification.
     */
    private void sendSystemNotification(String title, String body) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "App Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications for event updates and announcements.");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_radar)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    /**
     * Called when a new FCM registration token is generated.
     * This can occur when the app is installed for the first time, when the user clears
     * app data, or when the token expires. The new token should be sent to the
     * application server to associate it with the user.
     *
     * @param token The new FCM registration token.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
        // Here you would typically send the new token to your server to store it against the user's profile.
    }
}
