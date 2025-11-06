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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.rocket.radar.R;
import com.rocket.radar.profile.ProfileModel;

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
        // Use MutableLiveData to be able to post values to it.
        MutableLiveData<List<Notification>> notificationsLiveData = new MutableLiveData<>();

        // If the user reference was not set in the constructor (user not logged in), post an empty list.
        if (userNotificationsRef == null) {
            Log.e(TAG, "User is not logged in, cannot fetch notifications.");
            notificationsLiveData.postValue(new ArrayList<>());
            return notificationsLiveData;
        }

        // Use addSnapshotListener to get real-time updates. Every time a notification's
        // readStatus changes, this code will re-run automatically.
        userNotificationsRef.addSnapshotListener((userNotificationsSnapshot, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed on user notifications.", error);
                return;
            }

            // Handle the case where the user has no notification documents.
            if (userNotificationsSnapshot == null || userNotificationsSnapshot.isEmpty()) {
                notificationsLiveData.postValue(new ArrayList<>());
                return;
            }

            List<Task<Notification>> tasks = new ArrayList<>();
            Map<String, Boolean> readStatusMap = new HashMap<>();

            for (QueryDocumentSnapshot userDoc : userNotificationsSnapshot) {
                DocumentReference notificationContentRef = userDoc.getDocumentReference("notificationRef");
                if (notificationContentRef != null) {
                    // For each notification stub, create a Task to fetch its full content.
                    Task<Notification> fetchTask = notificationContentRef.get().continueWith(contentTask -> {
                        if (contentTask.isSuccessful() && contentTask.getResult() != null && contentTask.getResult().exists()) {
                            Notification notification = contentTask.getResult().toObject(Notification.class);
                            if (notification != null) {
                                notification.setUserNotificationId(userDoc.getId());
                                return notification;
                            }
                        }
                        // If the fetch fails for any reason, return null for this task.
                        Log.e(TAG, "Failed to fetch content for notification: " + userDoc.getId(), contentTask.getException());
                        return null;
                    });
                    tasks.add(fetchTask);

                    // Store the read status, keyed by the user-specific notification ID.
                    readStatusMap.put(userDoc.getId(), Boolean.TRUE.equals(userDoc.getBoolean("readStatus")));
                }
            }

            // Use Tasks.whenAllSuccess to wait for ALL fetch tasks to complete successfully.
            Tasks.whenAllSuccess(tasks).addOnSuccessListener(results -> {
                List<Notification> finalNotifications = new ArrayList<>();
                for (Object result : results) {
                    // The result list contains Notification objects (or nulls if any failed).
                    if (result instanceof Notification) {
                        Notification notification = (Notification) result;
                        // Look up the correct read status from our map.
                        boolean isRead = readStatusMap.getOrDefault(notification.getUserNotificationId(), false);
                        notification.setReadStatus(isRead);
                        finalNotifications.add(notification);
                    }
                }
                // Post the final, complete list to LiveData. The fragment will receive this update.
                notificationsLiveData.postValue(finalNotifications);
                Log.d(TAG, "Successfully fetched and processed " + finalNotifications.size() + " notifications.");
            });
        });

        return notificationsLiveData;
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
     *                   (e.g., "attendees", "onWaitlistEventIds").
     */
    // C:/Users/bwood/Cmput301/rocket-radar/src/app/src/main/java/com/rocket/radar/notifications/NotificationRepository.java

    public void sendNotificationToGroup(String title, String body, String eventId, String groupCollection) {
        if (eventId == null || eventId.isEmpty() || groupCollection == null || groupCollection.isEmpty()) {
            Log.e(TAG, "Event ID or group collection name is missing. Cannot send notification.");
            return;
        }

        // --- START OF REFACTOR ---
        // 1. Fetch the user IDs from the specified sub-collection of the event.
        // The path is: events/{eventId}/{groupCollection}
        db.collection("events").document(eventId).collection(groupCollection).get()
                .addOnSuccessListener(userCollectionSnapshot -> {
                    if (userCollectionSnapshot.isEmpty()) {
                        Log.w(TAG, "No users found in collection '" + groupCollection + "' for event " + eventId);
                        return;
                    }

                    // 2. Extract the user IDs. The document ID in the sub-collection *is* the user ID.
                    List<String> userIds = new ArrayList<>();
                    for (QueryDocumentSnapshot userDoc : userCollectionSnapshot) {
                        userIds.add(userDoc.getId());
                    }

                    Log.d(TAG, "Found " + userIds.size() + " users in collection '" + groupCollection + "'. Preparing notification.");

                    // 3. Create the main notification content.
                    Map<String, Object> newNotificationContent = new HashMap<>();
                    newNotificationContent.put("eventTitle", title);
                    newNotificationContent.put("notificationType", body);
                    newNotificationContent.put("image", R.drawable.ic_radar); // Assuming this is a valid drawable
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
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully fanned out notification to group '" + groupCollection + "'."))
                                        .addOnFailureListener(e -> Log.e(TAG, "Failed to commit batch for group notification.", e));

                            }).addOnFailureListener(e -> Log.e(TAG, "Failed to create main notification content.", e));

                }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch users from collection '" + groupCollection + "' for event: " + eventId, e));
        // --- END OF REFACTOR ---
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
