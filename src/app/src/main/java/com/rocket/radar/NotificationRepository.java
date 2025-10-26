package com.rocket.radar;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for handling all data operations related to Notifications.
 * This class acts as a single source of truth for notification data and abstracts the
 * data source (Firestore) from the rest of the application, such as UI controllers
 * and fragments. This makes the app easier to test and maintain.
 * <p>
 * Author: Braden Woods
 */
public class NotificationRepository {

    // A reference to the Firestore database instance.
    // 'final' ensures it is initialized once and not changed later.
    private final FirebaseFirestore db;

    // A constant for the 'users' collection name to avoid magic strings.
    private static final String COLLECTION_USERS = "users";

    // A constant for the 'notifications' sub-collection name.
    private static final String SUBCOLLECTION_NOTIFICATIONS = "notifications";

    /**
     * Constructor for the NotificationRepository.
     * Initializes the connection to the Firestore database.
     */
    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches all notifications for a specific user, ordered by most recent first.
     * This performs a SINGLE fetch from the database.
     *
     * @param userId The ID of the user whose notifications are to be fetched.
     * @return A Task that, upon completion, will contain the QuerySnapshot of notifications.
     */
    public Task<QuerySnapshot> getNotificationsForUser(String userId) {
        // Build the query to get documents from the user's specific sub-collection.
        // e.g., /users/{userId}/notifications
        return db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_NOTIFICATIONS)
                // Order by the 'timestamp' field in descending order to show the newest notifications first.
                .orderBy("timestamp", Query.Direction.DESCENDING)
                // .get() executes the query a single time.
                .get();
    }

    /**
     * Attaches a real-time listener to the notifications sub-collection for a specific user.
     * The provided callback will be invoked initially and then every time the data changes.
     *
     * @param userId The ID of the user whose notifications are to be monitored.
     * @param listener The callback to be triggered with data updates or errors.
     * @return A ListenerRegistration object that can be used to detach the listener later.
     */
    public ListenerRegistration addSnapshotListenerForUserNotifications(String userId,
                                                                        EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_NOTIFICATIONS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener); // The key change: .addSnapshotListener instead of .get()
    }

    /**
     * Creates and sends a single notification object to a list of specified users.
     * This method uses a Firestore WriteBatch to ensure the operation is atomic:
     * either all notifications are created successfully, or none are. This prevents
     * partial states where only some users receive the notification.
     *
     * @param notification     The Notification object to send. The same object is written for each user.
     * @param recipientUserIds A list of user IDs who will receive the notification.
     * @return A Task<Void> that completes when the entire batch write operation is finished.
     */
    public Task<Void> sendNotification(Notification notification, List<String> recipientUserIds) {
        // Create a new WriteBatch for this specific operation.
        WriteBatch batch = db.batch();

        for (String userId : recipientUserIds) {
            // For each recipient, create a new document reference inside their personal
            // 'notifications' sub-collection. Calling .document() with no arguments
            // generates a new, unique ID for the notification document.
            DocumentReference userNotifDoc = db.collection(COLLECTION_USERS).document(userId)
                    .collection(SUBCOLLECTION_NOTIFICATIONS).document();

            // Add a 'set' operation to the batch. This stages the write without sending it yet.
            batch.set(userNotifDoc, notification);
        }

        // Atomically commit all the operations in the batch.
        // The Task completes once the server has confirmed the write.
        return batch.commit();
    }

    /**
     * Marks a specific notification as read in Firestore by setting its 'isRead' field to true.
     * This method is idempotent, meaning calling it multiple times on the same notification
     * has no additional effect beyond the first call.
     *
     * @param userId         The ID of the user who owns the notification.
     * @param notificationId The ID of the specific notification document to update.
     * @return A Task<Void> that completes when the update is finished.
     */
    public Task<Void> markNotificationAsRead(String userId, String notificationId) {
        // Create a Map to define the update. This is more efficient for changing
        // a small number of fields than fetching and re-setting the entire object.
        Map<String, Object> updates = new HashMap<>();
        updates.put("isRead", true);

        // Get a reference to the specific notification document to be updated.
        DocumentReference notifDocRef = db.collection(COLLECTION_USERS).document(userId)
                .collection(SUBCOLLECTION_NOTIFICATIONS).document(notificationId);

        // Perform the update using .set() with SetOptions.merge().
        // merge() is crucial here: it updates only the fields specified in the 'updates' map
        // and leaves all other fields in the document untouched. Without it, .set() would
        // overwrite and delete all other data in the document.
        return notifDocRef.set(updates, SetOptions.merge());
    }
}
