package com.rocket.radar;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

// ... (class documentation)
public class NotificationController {

    // ... (fields and constructors are correct)
    private final NotificationRepository repository;
    private final FirebaseFirestore db;

    public NotificationController() {
        this.db = FirebaseFirestore.getInstance();
        this.repository = new NotificationRepository();
    }

    public NotificationController(FirebaseFirestore db, NotificationRepository repository) {
        this.db = db;
        this.repository = repository;
    }

    // ... (public methods like sendNotificationToSelectedEntrants are correct)
    public Task<Void> sendNotificationToSelectedEntrants(String eventId, String senderId, List<String> entrantIds) {
        String title = "You're In!";
        String description = "Congratulations! You have been selected to attend the event.";
        return fetchEventImageAndSend(eventId, senderId, entrantIds, title, description,
                NotificationType.SELECTED_ENTRANTS, RecipientGroup.SELECTED);
    }

    public Task<Void> sendNotificationToWaitlistedEntrants(String eventId, String senderId, List<String> entrantIds) {
        String title = "You're on the Waitlist";
        String description = "You've been added to the waitlist. We'll notify you if a spot opens up!";
        return fetchEventImageAndSend(eventId, senderId, entrantIds, title, description,
                NotificationType.WAITLIST_STATUS, RecipientGroup.SELECTED);
    }

    public Task<Void> sendAnnouncementToUsers(String eventId, String senderId, List<String> recipientIds,
                                              String title, String description) {
        return fetchEventImageAndSend(eventId, senderId, recipientIds, title, description,
                NotificationType.EVENT_DETAILS, RecipientGroup.ALL);
    }

    // ... inside NotificationController class

    private Task<Void> fetchEventImageAndSend(String eventId, String senderId, List<String> recipientIds,
                                              String title, String description,
                                              NotificationType type, RecipientGroup group) {

        return db.collection("events").document(eventId).get().continueWithTask(
                (Task<DocumentSnapshot> task) -> {

                    // First, check if the task itself failed (e.g., network issue)
                    if (!task.isSuccessful()) {
                        // Propagate the original exception
                        return Tasks.forException(task.getException() != null ?
                                task.getException() : new Exception("Failed to fetch event document."));
                    }

                    DocumentSnapshot eventDoc = task.getResult();

                    // *** THIS IS THE CRITICAL FIX ***
                    // Explicitly check if the document exists before trying to access its data.
                    if (eventDoc == null || !eventDoc.exists()) {
                        // If the document doesn't exist, fail the entire task with a clear error.
                        // This prevents the SDK from crashing on a null reference.
                        return Tasks.forException(new Exception("Event document not found for eventId: " + eventId));
                    }

                    // Now that we know the document exists, it's safe to get data from it.
                    String imageUrl = eventDoc.getString("eventImageUrl");

                    Notification notification = new Notification(
                            title, description, eventId, type.name(),
                            group.getValue(), senderId, imageUrl
                    );

                    // Now call the repository to send the notification
                    return repository.sendNotification(notification, recipientIds);
                });
    }
}
