package com.rocket.radar.notifications;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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
     * @param groupCollection The field in the event document that contains the list of user IDs
     *                   (e.g., "attendees", "onWaitlistEventIds").
     */
    public void sendNotificationToGroup(String title, String body, String eventId, String groupCollection) {
        if (eventId == null || eventId.isEmpty() || groupCollection == null || groupCollection.isEmpty()) {
            Log.e(TAG, "Event ID or group collection name is missing. Cannot send notification.");
            return;
        }

        // 1. Fetch the user IDs from the specified sub-collection of the event.
        db.collection("events").document(eventId).collection(groupCollection).get()
                .addOnSuccessListener(userCollectionSnapshot -> {
                    if (userCollectionSnapshot.isEmpty()) {
                        Log.w(TAG, "No users found in collection '" + groupCollection + "' for event " + eventId);
                        return;
                    }

                    List<String> userIds = new ArrayList<>();
                    for (QueryDocumentSnapshot userDoc : userCollectionSnapshot) {
                        userIds.add(userDoc.getId());
                    }

                    // --- START OF FIX ---

                    // 2. Create a list of tasks to fetch each user's profile document.
                    List<Task<DocumentSnapshot>> userFetchTasks = new ArrayList<>();
                    for (String userId : userIds) {
                        userFetchTasks.add(db.collection("users").document(userId).get());
                    }

                    // 3. Wait for ALL user profile fetches to complete.
                    Tasks.whenAllSuccess(userFetchTasks).addOnSuccessListener(userSnapshots -> {
                        // This list contains DocumentSnapshot objects for each user.

                        // 4. Filter the list to get only the users with notifications enabled.
                        List<String> usersToNotify = new ArrayList<>();
                        for (Object snapshot : userSnapshots) {
                            DocumentSnapshot userDoc = (DocumentSnapshot) snapshot;

                            // This is the check. It defaults to 'true' if the field doesn't exist.
                            // It only skips if the field exists and is explicitly 'false'.
                            if (!Boolean.FALSE.equals(userDoc.getBoolean("notificationsEnabled"))) {
                                usersToNotify.add(userDoc.getId());
                            } else {
                                Log.d(TAG, "Skipping user " + userDoc.getId() + " because they have notifications disabled.");
                            }
                        }

                        if (usersToNotify.isEmpty()) {
                            Log.d(TAG, "No users in the group have notifications enabled. Aborting.");
                            return;
                        }

                        Log.d(TAG, "Preparing to send notification to " + usersToNotify.size() + " enabled users.");

                        // 5. Create and fan out the notification ONLY to the filtered list.
                        createAndFanOutNotification(title, body, usersToNotify);

                    }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch one or more user profiles.", e));
                    // --- END OF FIX ---

                }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch users from collection '" + groupCollection + "' for event: " + eventId, e));
    }

    /**
     * Helper method to create the main notification content and fan it out to the specified users.
     */
    private void createAndFanOutNotification(String title, String body, List<String> usersToNotify) {
        Map<String, Object> newNotificationContent = new HashMap<>();
        newNotificationContent.put("eventTitle", title);
        newNotificationContent.put("notificationType", body);
        newNotificationContent.put("image", R.drawable.ic_radar);
        newNotificationContent.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notifications").add(newNotificationContent)
                .addOnSuccessListener(contentRef -> {
                    // Create a batch write to fan out the notification to all ELIGIBLE users.
                    WriteBatch batch = db.batch();
                    for (String userId : usersToNotify) {
                        DocumentReference userStubRef = db.collection("users").document(userId)
                                .collection("notifications").document();

                        Map<String, Object> userStub = new HashMap<>();
                        userStub.put("readStatus", false);
                        userStub.put("notificationRef", contentRef);
                        batch.set(userStubRef, userStub);
                    }

                    // Commit the batch.
                    batch.commit()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully fanned out notification to " + usersToNotify.size() + " users."))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to commit batch for notification fan-out.", e));

                }).addOnFailureListener(e -> Log.e(TAG, "Failed to create main notification content.", e));
    }


}
