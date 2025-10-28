package com.rocket.radar;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

/**
 * Handles all data operations related to notifications in Firestore.
 * This class is now fully testable via dependency injection.
 * <p>
 * Author: Braden Woods
 */
public class NotificationRepository {

    private final FirebaseFirestore db;

    /**
     * Public constructor for main application use.
     */
    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Constructor for testing, allowing a mock Firestore instance to be injected.
     * @param db A mock or real FirebaseFirestore instance.
     */
    public NotificationRepository(FirebaseFirestore db) {
        this.db = db;
    }

    // --- Methods are now instance methods and use the 'db' field ---

    public Task<Void> sendNotification(Notification notification, List<String> recipientIds) {
        WriteBatch batch = db.batch();
        for (String userId : recipientIds) {
            CollectionReference userNotifCollection = db.collection("users").document(userId).collection("notifications");
            batch.set(userNotifCollection.document(), notification);
        }
        return batch.commit();
    }

    public Task<Void> markNotificationAsRead(String userId, String notificationId) {
        return db.collection("users").document(userId).collection("notifications")
                .document(notificationId)
                .update("read", true);
    }

    public ListenerRegistration addSnapshotListenerForUserNotifications(String userId, EventListener<QuerySnapshot> listener) {
        return db.collection("users").document(userId).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(listener);
    }

    public Task<Void> clearAllNotificationsForUser(String userId) {
        return db.collection("users").document(userId).collection("notifications")
                .get()
                .onSuccessTask(querySnapshot -> {
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    return batch.commit();
                });
    }
}
