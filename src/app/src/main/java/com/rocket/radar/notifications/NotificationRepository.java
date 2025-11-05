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
     * Creates a test notification. This simulates what a server function would do.
     * 1. Creates a document in the top-level 'notifications' collection.
     * 2. Creates a "stub" in the current user's sub-collection that points to the main document.
     */
    public void createTestNotificationForCurrentUser(String title, String body) {
        if (userNotificationsRef == null) {
            Log.e(TAG, "Cannot create test notification, user is not logged in.");
            return;
        }

        // 1. Create the main notification content
        Map<String, Object> newNotificationContent = new HashMap<>();
        newNotificationContent.put("eventTitle", title);
        newNotificationContent.put("notificationType", body);
        newNotificationContent.put("image", 1); // Placeholder image
        newNotificationContent.put("timestamp", FieldValue.serverTimestamp());

        db.collection("notifications")
                .add(newNotificationContent) // .add() creates a unique ID
                .addOnSuccessListener(contentRef -> {
                    Log.d(TAG, "Successfully created main notification with ID: " + contentRef.getId());

                    // 2. Create the user-specific stub linking to the main notification
                    Map<String, Object> userNotificationStub = new HashMap<>();
                    userNotificationStub.put("readStatus", false);
                    userNotificationStub.put("notificationRef", contentRef); // Store the reference

                    userNotificationsRef.add(userNotificationStub)
                            .addOnSuccessListener(stubRef -> Log.d(TAG, "Successfully linked notification to user."))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to link notification to user", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to create main notification content", e));
    }
}
