package com.rocket.radar.notifications;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.rocket.radar.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference userNotificationsRef; // This will point to users/{uid}/notifications

    public NotificationRepository() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Set the reference to the specific user's notification sub-collection
            this.userNotificationsRef = db.collection("users").document(currentUser.getUid()).collection("notifications");
        }
    }

    public LiveData<List<Notification>> getMyNotifications() {
        MutableLiveData<List<Notification>> resolvedNotificationsLiveData = new MutableLiveData<>();
        if (userNotificationsRef == null) {
            Log.e(TAG, "User is not logged in. Cannot fetch notifications.");
            resolvedNotificationsLiveData.postValue(new ArrayList<>());
            return resolvedNotificationsLiveData;
        }

        userNotificationsRef.addSnapshotListener((userNotificationsSnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed on user notifications.", error);
                return;
            }
            if (userNotificationsSnapshot == null || userNotificationsSnapshot.isEmpty()) {
                resolvedNotificationsLiveData.postValue(new ArrayList<>()); // Post empty list if no notifications
                return;
            }

            List<Notification> resolvedList = new ArrayList<>();
            // Use an atomic counter to know when all async fetches are complete
            AtomicInteger pendingFetches = new AtomicInteger(userNotificationsSnapshot.size());

            for (QueryDocumentSnapshot userDoc : userNotificationsSnapshot) {
                // Get the reference from the user's notification stub
                DocumentReference notificationContentRef = userDoc.getDocumentReference("notificationRef");
                boolean readStatus = Boolean.TRUE.equals(userDoc.getBoolean("readStatus"));

                if (notificationContentRef != null) {
                    notificationContentRef.get().addOnSuccessListener(contentDoc -> {
                        if (contentDoc.exists()) {
                            Notification notification = contentDoc.toObject(Notification.class);
                            if (notification != null) {
                                // Manually set the UI-specific fields
                                notification.setUserNotificationId(userDoc.getId());
                                notification.setReadStatus(readStatus);
                                resolvedList.add(notification);
                            }
                        }
                        // Decrement counter and check if all fetches are done
                        if (pendingFetches.decrementAndGet() == 0) {
                            resolvedNotificationsLiveData.postValue(resolvedList);
                        }
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch notification content.", e);
                        if (pendingFetches.decrementAndGet() == 0) {
                            resolvedNotificationsLiveData.postValue(resolvedList);
                        }
                    });
                } else {
                    // If ref is null, just decrement and continue
                    if (pendingFetches.decrementAndGet() == 0) {
                        resolvedNotificationsLiveData.postValue(resolvedList);
                    }
                }
            }
        });
        return resolvedNotificationsLiveData;
    }

    public void markNotificationAsRead(String userNotificationId) {
        if (userNotificationsRef == null || userNotificationId == null) return;
        userNotificationsRef.document(userNotificationId)
                .update("readStatus", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification marked as read: " + userNotificationId))
                .addOnFailureListener(e -> Log.e(TAG, "Error marking notification as read", e));
    }

    /**
     * Sends a notification to a specific group of users associated with an event (e.g., attendees).
     * This method first reads the event document to get the list of user IDs.
     *
     * @param title      The title of the notification.
     * @param body       The body text of the notification.
     * @param eventId    The ID of the event to get the user group from.
     * @param groupField The field in the event document that contains the list of user IDs
     *                   (e.g., "attendees", "onWaitlistEventIds").
     */
    public void sendNotificationToGroup(String title, String body, String eventId, String groupField) {
        if (eventId == null || eventId.isEmpty() || groupField == null || groupField.isEmpty()) {
            Log.e(TAG, "Event ID or group field is missing. Cannot send notification.");
            return;
        }

        // 1. First, fetch the event document to find the list of users.
        db.collection("events").document(eventId).get().addOnSuccessListener(eventDoc -> {
            if (!eventDoc.exists()) {
                Log.e(TAG, "Event with ID " + eventId + " not found.");
                return;
            }

            // 2. Get the list of user IDs from the specified field.
            List<String> userIds = (List<String>) eventDoc.get(groupField);

            if (userIds == null || userIds.isEmpty()) {
                Log.w(TAG, "No users found in group '" + groupField + "' for event " + eventId);
                return;
            }

            Log.d(TAG, "Found " + userIds.size() + " users in group '" + groupField + "'. Preparing notification.");

            // 3. Create the main notification content.
            Map<String, Object> newNotificationContent = new HashMap<>();
            newNotificationContent.put("eventTitle", title);
            newNotificationContent.put("notificationType", body);
            newNotificationContent.put("image", R.drawable.ic_radar);
            newNotificationContent.put("timestamp", FieldValue.serverTimestamp());

            db.collection("notifications").add(newNotificationContent)
                    .addOnSuccessListener(contentRef -> {
                        Log.d(TAG, "Successfully created main notification with ID: " + contentRef.getId());

                        // 4. Fan out the notification reference to all users in the group.
                        WriteBatch batch = db.batch();
                        for (String userId : userIds) {
                            if (userId != null && !userId.isEmpty()) {
                                DocumentReference userStubRef = db.collection("users").document(userId)
                                        .collection("notifications").document();
                                Map<String, Object> userStub = new HashMap<>();
                                userStub.put("readStatus", false);
                                userStub.put("notificationRef", contentRef);
                                batch.set(userStubRef, userStub);
                            }
                        }

                        batch.commit()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully fanned out notification to group '" + groupField + "'."))
                                .addOnFailureListener(e -> Log.e(TAG, "Failed to commit batch for group notification.", e));

                    }).addOnFailureListener(e -> Log.e(TAG, "Failed to create main notification content.", e));

        }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch event document: " + eventId, e));
    }

    /*

        How to use this:

        // Somewhere in your app where an organizer sends an announcement...

        // 1. You only need the event ID and the target group.
        String currentEventId = "some_event_id_12345";
        String targetGroup = "attendees"; // Or "onWaitlistEventIds", or any other field in your event document.

        // 2. Define the notification message.
        String notificationTitle = "Important Update";
        String notificationBody = "The event location has been changed. Please check the details.";

        // 3. Create a repository instance and call the new, smarter method.
        NotificationRepository repository = new NotificationRepository();
        repository.sendNotificationToGroup(notificationTitle, notificationBody, currentEventId, targetGroup);

     */

}
