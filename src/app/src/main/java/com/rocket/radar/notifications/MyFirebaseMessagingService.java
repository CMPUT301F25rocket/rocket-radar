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
// This import is no longer needed as the repository handles its own logic.
// import com.rocket.radar.notifications.NotificationRepository;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFCMService";
    private static final String CHANNEL_ID = "event_notifications_channel";

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
            addNotificationToInAppList(title, body);
        }
    }

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
     * Corrected method to add the notification to the in-app list.
     * This now correctly uses the repository's method.
     */
    private void addNotificationToInAppList(String title, String body) {
        // The service needs to know which user is logged in to add the notification stub.
        // A repository created here will correctly check for the current authenticated user.
        NotificationRepository repository = new NotificationRepository();

        // We use the method designed for this purpose, which handles both creating the
        // main notification and linking it to the current user.
        repository.createTestNotificationForCurrentUser(title, body);
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed FCM Token: " + token);
        // Here you would typically send the new token to your server to store it against the user's profile.
    }
}
