package com.rocket.radar;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

/**
 * A static controller class that acts as the business logic layer for creating notifications.
 * It simplifies the process of sending notifications by automatically fetching necessary
 * event details (like the image URL) before constructing and sending the notification
 * via the NotificationRepository.
 * <p>
 * Author: Braden Woods
 */
public class NotificationController {

    // A static repository instance for sending the notifications once they are built.
    private static final NotificationRepository repository = new NotificationRepository();

    // A direct reference to Firestore is now needed in the controller to fetch event details.
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Sends a "You're In!" notification to selected event entrants.
     * This method automatically fetches the event's image URL.
     *
     * @param eventId    The ID of the event. This is used to fetch the event's image.
     * @param senderId   The ID of the user (organizer) sending the notification.
     * @param entrantIds A list of user IDs for the selected entrants.
     * @return A Task that completes when the operation is finished.
     */
    public static Task<Void> sendNotificationToSelectedEntrants(String eventId,
                                                                String senderId,
                                                                List<String> entrantIds) {
        String title = "You're In!";
        String description = "Congratulations! You have been selected to attend the event.";

        // Delegate to the private helper method that handles fetching and sending.
        return fetchEventImageAndSend(eventId, senderId, entrantIds, title, description,
                NotificationType.SELECTED_ENTRANTS, RecipientGroup.SELECTED);
    }

    /**
     * Sends a "Waitlisted" notification to entrants.
     * This method automatically fetches the event's image URL.
     *
     * @param eventId    The ID of the event used to fetch the image.
     * @param senderId   The ID of the user (organizer) sending the notification.
     * @param entrantIds A list of user IDs for the waitlisted entrants.
     * @return A Task that completes when the operation is finished.
     */
    public static Task<Void> sendNotificationToWaitlistedEntrants(String eventId,
                                                                  String senderId,
                                                                  List<String> entrantIds) {
        String title = "You're on the Waitlist";
        String description = "You've been added to the waitlist. We'll notify you if a spot opens up!";

        // Delegate to the private helper method.
        return fetchEventImageAndSend(eventId, senderId, entrantIds, title, description,
                NotificationType.WAITLIST_STATUS, RecipientGroup.SELECTED);
    }

    /**
     * Sends a generic announcement to a list of users.
     * This method automatically fetches the event's image URL.
     *
     * TODO: This method will be used when an organizer sends a mass message to all attendees.
     *       The UI for this feature still needs to be built.
     *
     * @param eventId     The ID of the event used to fetch the image.
     * @param senderId    The ID of the user (organizer) sending the notification.
     * @param recipientIds A list of all user IDs who should receive this announcement.
     * @param title       The custom title of the announcement.
     * @param description The custom body of the announcement.
     * @return A Task that completes when the operation is finished.
     */
    public static Task<Void> sendAnnouncementToUsers(String eventId, String senderId, List<String> recipientIds,
                                                     String title, String description) {
        // Delegate to the private helper method.
        return fetchEventImageAndSend(eventId, senderId, recipientIds, title, description,
                NotificationType.EVENT_DETAILS, RecipientGroup.ALL);
    }


    /**
     * Private helper method that performs the two-step process of fetching event details
     * and then sending the notification. This is the core of the new logic.
     *
     * @return A Task that chains the fetch and send operations.
     */
    private static Task<Void> fetchEventImageAndSend(String eventId, String senderId, List<String> recipientIds,
                                                     String title, String description,
                                                     NotificationType type, RecipientGroup group) {
        // Step 1: Fetch the Event document from the 'events' collection.
        // TODO: Replace "events" with your actual events collection name if it's different.
        // TODO: Replace "eventImageUrl" with the actual field name for the image URL in your Event model.
        return db.collection("events").document(eventId).get().continueWithTask(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                // If fetching the event fails, we can either send the notification with no image
                // or fail the entire operation. Failing is safer to signal an issue.
                return Tasks.forException(task.getException() != null ?
                        task.getException() : new Exception("Event document not found."));
            }

            DocumentSnapshot eventDoc = task.getResult();
            String imageUrl = eventDoc.getString("eventImageUrl"); // Extracts the URL string.

            // Step 2: Create the Notification object with the fetched imageUrl.
            Notification notification = new Notification(
                    title,
                    description,
                    eventId,
                    type.name(),
                    group.getValue(),
                    senderId,
                    imageUrl // The fetched URL is used here!
            );

            // Step 3: Delegate the sending logic to the repository.
            return repository.sendNotification(notification, recipientIds);
        });
    }
}
