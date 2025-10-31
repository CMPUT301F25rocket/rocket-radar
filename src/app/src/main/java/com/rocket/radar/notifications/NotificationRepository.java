package com.rocket.radar.notifications;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles logic notification db logic
 */
public class NotificationRepository {

    private static final String TAG = "NotificationRepository";
    private final FirebaseFirestore db;
    private final CollectionReference notificationRef;

    // This constructor now correctly initializes Firestore.
    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.notificationRef = db.collection("notifications"); // Use "notifications" collection
    }

    public LiveData<List<Notification>> getAllNotifications() {
        MutableLiveData<List<Notification>> notificationLiveData = new MutableLiveData<>();

        notificationRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed.", error);
                return;
            }

            ArrayList<Notification> notificationList = new ArrayList<>();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    // Convert each document into an Notification object
                    Notification notification = doc.toObject(Notification.class);
                    notificationList.add(notification);
                }
            }
            // Post the new list to observers (like your fragment)
            notificationLiveData.postValue(notificationList);
        });

        return notificationLiveData;
    }

    // This method adds a new notification to Firestore.
    public void createNotification(Notification notification) {
        // Use the Notification's title as the document ID for simplicity, or use .add() for auto-ID
        notificationRef.document(notification.getEventTitle()).set(notification)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification successfully written: " + notification.getEventTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Error writing notification", e));
    }
}
