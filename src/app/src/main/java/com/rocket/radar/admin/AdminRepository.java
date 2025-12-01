package com.rocket.radar.admin;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rocket.radar.R;
import com.rocket.radar.notifications.Notification;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.events.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for admin related firestore functions in the data layer.
 * This includes fetching users, deleting users, deleting events.
 * Notifications also get sent when some of these admin actions occur, like updating a user role or deleting an event.
 */
public class AdminRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Gets all the users in the collection and populates a list of profile models for each user.
     * @param listener custom interface defining logic that occurs when the get finishes.
     */
    public void getAllUsers(OnCompleteListener<List<ProfileModel>> listener) {
        db.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<ProfileModel> profiles = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        ProfileModel profile = doc.toObject(ProfileModel.class);
                        profile.setUid(doc.getId());
                        profiles.add(profile);
                        Log.d("AdminRepository", "Loaded user: " + profile.getName() + ", UID: " + profile.getUid());
                    }
                    listener.onComplete(profiles);
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Error fetching users", e);
                    listener.onComplete(new ArrayList<>());
                });
    }

    /**
     * A listener interface for receiving a callback when an asynchronous
     * getAllUsers operation has completed.
     * @param <T> The type of the result returned when the operation completes
     */
    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }

    /**
     * This function deletes a given user, including removing their id from all event related sub collections.
     * This also deletes all the notifications references that are stored as a subcollection with the user id.
     * @param profile the user to delete.
     * @param callback the logic to happen when the function completes.
     */
    public void deleteUser(ProfileModel profile, DeleteUserCallback callback) {
        String uid = profile.getUid();

        removeUserFromAllEvents(uid, () -> {
            db.collection("users")
                    .document(uid)
                    .collection("notifications")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (var document : querySnapshot.getDocuments()) {
                            document.getReference().delete();
                        }
                        db.collection("users")
                                .document(uid)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("AdminRepository", "User deleted successfully: " + uid);
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("AdminRepository", "Failed to delete user doc", e);
                                    callback.onError(e);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminRepository", "Failed to fetch notifications", e);
                        callback.onError(e);
                    });
        }, callback::onError);
    }

    /**
     * Removes all references to the given user from every event's membership subcollections.
     *
     * @param uid The ID of the user being removed.
     * @param onComplete Called once deletions have been requested.
     * @param onError Called if any deletion request fails.
     */
    public void removeUserFromAllEvents(String uid, Runnable onComplete, OnErrorCallback onError) {
        String[] subcollections = {"attendingUsers", "waitlistedUsers", "invitedUsers", "cancelledUsers"};

        db.collection("events")
                .get()
                .addOnSuccessListener(eventsSnapshot -> {
                    for (var eventDoc : eventsSnapshot.getDocuments()) {
                        String eventId = eventDoc.getId();

                        for (String sub : subcollections) {
                            db.collection("events")
                                    .document(eventId)
                                    .collection(sub)
                                    .document(uid) // doc ID is the user ID
                                    .delete()
                                    .addOnFailureListener(e -> {
                                        Log.e("Cleanup", "Failed to delete user " + uid
                                                + " from " + sub + " in event " + eventId, e);
                                        onError.onError(e);
                                    });
                        }
                    }

                    onComplete.run();
                })
                .addOnFailureListener(onError::onError);
    }


    /**
     * Callback interface for deleting a user.
     */
    public interface DeleteUserCallback {
        /**
         * Logic for when delete is successful.
         */
        void onSuccess();

        /**
         * Logic for when delete fails.
         * @param e Exception that occured.
         */
        void onError(Exception e);
    }

    /**
     * Updates the role for the user using the ProfileModel state enum.
     * @param profile profile to update.
     * @param role the new role to set.
     * @param callback logic for when the update finished.
     */
    public void updateUserRole(ProfileModel profile, ProfileModel.UserRole role, updateCallback callback) {
        String uid = profile.getUid();
        ProfileModel.UserRole oldRole = profile.getRole();

        // If changing TO entrant from a role that can create events
        if (role == ProfileModel.UserRole.ENTRANT &&
                (oldRole == ProfileModel.UserRole.ORGANIZER || oldRole == ProfileModel.UserRole.ADMIN)) {

            ArrayList<String> eventIds = profile.getOnMyEventIds();

            // Delete all events in the user's onMyEventIds list
            deleteUserEvents(uid, eventIds, () -> {
                // After events are deleted, update the role
                db.collection("users")
                        .document(uid)
                        .update("role", role)
                        .addOnSuccessListener(aVoid -> {
                            // Send notification about role change
                            sendRoleChangeNotification(uid, role);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("AdminRepository", "Failed to update user role", e);
                            callback.onError(e);
                        });
            }, callback::onError);
        } else {
            // No event deletion needed, just update role
            db.collection("users")
                    .document(uid)
                    .update("role", role)
                    .addOnSuccessListener(aVoid -> {
                        // Send notification about role change
                        sendRoleChangeNotification(uid, role);
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminRepository", "Failed to update user role", e);
                        callback.onError(e);
                    });
        }
    }

    /**
     * Callback for when update user role finishes.
     */
    public interface updateCallback {
        /**
         * Logic for success.
         */
        void onSuccess();

        /**
         * Logic for update fail.
         * @param e the Exception that occurred.
         */
        void onError(Exception e);
    }

    /**
     * This function deletes a given event.
     * It removes the event from all users.
     * It removes all notifications about the event.
     * It deletes all of the events subcollections.
     * It sends notifications to everyone associated with the event.
     * @param eventId The id of the event to delete.
     * @param callback Logic for when the delete event finishes.
     */
    public void deleteEvent(String eventId, DeleteEventCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty"));
            return;
        }

        // First, fetch the event to get its title and send notifications
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        callback.onError(new Exception("Event not found: " + eventId));
                        return;
                    }

                    Event event = eventDoc.toObject(Event.class);
                    String eventTitle = (event != null && event.getEventTitle() != null)
                            ? event.getEventTitle()
                            : "Event";

                    // Send notifications to all associated users BEFORE deleting the event
                    sendEventDeletedNotification(eventId, eventTitle);

                    // Now proceed with deletion
                    removeEventFromAllUsers(eventId, () -> {
                        removeNotificationsAboutEvent(eventId, () -> {
                            deleteEventSubcollections(eventId, () -> {
                                db.collection("events").document(eventId)
                                        .delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("AdminRepository", "Event deleted successfully: " + eventId);
                                            callback.onSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("AdminRepository", "Failed to delete event", e);
                                            callback.onError(e);
                                        });
                            }, callback::onError);
                        }, callback::onError);
                    }, callback::onError);
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Failed to fetch event for deletion", e);
                    callback.onError(e);
                });
    }

    /**
     * This function deletes all the subcollections stored at a given event document id.
     * @param eventId The event id
     * @param onSuccess Logic for when the delete is successful.
     * @param onError Logic for when the delete errors.
     */
    private void deleteEventSubcollections(String eventId, Runnable onSuccess, OnErrorCallback onError) {
        String[] subcollections = {"attendingUsers", "waitlistedUsers", "invitedUsers", "cancelledUsers"};

        for (String sub : subcollections) {
            db.collection("events").document(eventId).collection(sub)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (var doc : querySnapshot.getDocuments()) {
                            doc.getReference().delete();
                        }
                    })
                    .addOnFailureListener(onError::onError);
        }

        // After attempting to delete all subcollections, call onSuccess
        onSuccess.run();
    }

    /**
     * Removes an event from all user profiles that reference it.
     *  Scans all users and removes the event ID from their onMyEventIds list.
     * @param eventId the event to remove from all users.
     * @param onComplete Logic for when it completes.
     * @param onError Logic for when it fails.
     */
    private void removeEventFromAllUsers(String eventId, Runnable onComplete, OnErrorCallback onError) {
        db.collection("users").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ProfileModel profile = doc.toObject(ProfileModel.class);

                        if (profile.getOnMyEventIds() != null && profile.getOnMyEventIds().remove(eventId)) {
                            doc.getReference().update("onMyEventIds", profile.getOnMyEventIds())
                                    .addOnFailureListener(e -> {
                                        Log.e("AdminRepository", "Failed to update user " + doc.getId(), e);
                                        onError.onError(e);
                                    });
                        }
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Failed to query users for event removal", e);
                    onError.onError(e);
                });
    }

    /**
     * Removes all notifications associated with an event.
     * @param eventId the event id to remove notifications about.
     * @param onComplete logic for when the removal completes.
     * @param onError logic for when the removal errors.
     */
    private void removeNotificationsAboutEvent(String eventId, Runnable onComplete, OnErrorCallback onError) {
        db.collection("notifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Notification notification = doc.toObject(Notification.class);
                        if (notification != null && notification.getEventId() != null &&
                                notification.getEventId().equals(eventId)) {
                            doc.getReference().delete()
                                    .addOnFailureListener(e -> {
                                        Log.e("AdminRepository", "Failed to delete notification", e);
                                        onError.onError(e);
                                    });
                        }
                    }
                    Log.d("AdminRepository", "Notification cleanup complete for event: " + eventId);
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Failed to query notifications", e);
                    onError.onError(e);
                });
    }

    /**
     * This function deletes all the events a given user has created.
     * @param uid the user id to delete the user's events.
     * @param eventIds The event ids that the user has.
     * @param onComplete Logic for when the delete completes.
     * @param onError Logic for when the delete fails.
     */
    private void deleteUserEvents(String uid, ArrayList<String> eventIds, Runnable onComplete, OnErrorCallback onError) {
        if (eventIds == null || eventIds.isEmpty()) {
            onComplete.run();
            return;
        }

        final int[] remainingDeletes = {eventIds.size()};
        final boolean[] hasError = {false};

        for (String eventId : eventIds) {
            deleteEvent(eventId, new DeleteEventCallback() {
                @Override
                public void onSuccess() {
                    remainingDeletes[0]--;
                    if (remainingDeletes[0] == 0 && !hasError[0]) {
                        onComplete.run();
                    }
                }

                @Override
                public void onError(Exception e) {
                    hasError[0] = true;
                    Log.e("AdminRepository", "Failed to delete event: " + eventId, e);
                    onError.onError(e);
                }
            });
        }
    }

    /**
     * Callback interface for deleting an event.
     */
    public interface DeleteEventCallback {
        /**
         * Logic for success.
         */
        void onSuccess();

        /**
         * Logic for failure
         * @param e Exception
         */
        void onError(Exception e);
    }

    /**
     * General interface for errors.
     */
    public interface OnErrorCallback {
        /**
         * Logic for when an error is thrown.
         * @param e Exception
         */
        void onError(Exception e);
    }

    /**
     * Sends a role change notification to a specific user.
     * Creates a notification in the top-level collection and fans it out to the user's subcollection.
     *
     * @param userId The ID of the user to notify
     * @param newRole The new role that was assigned
     */
    private void sendRoleChangeNotification(String userId, ProfileModel.UserRole newRole) {
        // First check if the user has notifications enabled
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (Boolean.FALSE.equals(userDoc.getBoolean("notificationsEnabled"))) {
                        Log.d("AdminRepository", "User has notifications disabled. Skipping role change notification.");
                        return;
                    }

                    String userName = userDoc.getString("name");
                    if (userName == null || userName.isEmpty()) {
                        userName = "User"; // Fallback if name is not available
                    }

                    // Create the main notification content
                    Map<String, Object> notificationContent = new HashMap<>();
                    notificationContent.put("eventTitle", userName + " Role Updated");
                    notificationContent.put("notificationType", "An admin has changed your role to: " + newRole.name());
                    notificationContent.put("eventId", null); // No event associated
                    notificationContent.put("image", R.drawable.ic_radar);
                    notificationContent.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    // Add to top-level notifications collection
                    db.collection("notifications").add(notificationContent)
                            .addOnSuccessListener(notificationRef -> {
                                // Create the user-specific stub
                                Map<String, Object> userStub = new HashMap<>();
                                userStub.put("readStatus", false);
                                userStub.put("notificationRef", notificationRef);

                                // Add to user's notification subcollection
                                db.collection("users")
                                        .document(userId)
                                        .collection("notifications")
                                        .add(userStub)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("AdminRepository", "Role change notification sent to user: " + userId);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("AdminRepository", "Failed to add notification to user subcollection", e);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("AdminRepository", "Failed to create notification content", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Failed to check user notification settings", e);
                });
    }

    /**
     * Sends notifications to all users associated with an event when it is deleted.
     * This includes users from the subcollections: attendingUsers, waitlistedUsers, invitedUsers, and cancelledUsers.
     *
     * @param eventId The ID of the event being deleted
     * @param eventTitle The title of the event being deleted
     */
    private void sendEventDeletedNotification(String eventId, String eventTitle) {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(eventDoc -> {
                    if (!eventDoc.exists()) {
                        Log.e("AdminRepository", "Event not found: " + eventId);
                        return;
                    }

                    Event event = eventDoc.toObject(Event.class);
                    if (event == null) {
                        Log.e("AdminRepository", "Failed to parse event");
                        return;
                    }

                    // Get the organizer ID
                    String organizerId = event.getOrganizerId();

                    // Collect all user IDs from subcollections
                    java.util.HashSet<String> allUserIds = new java.util.HashSet<>();

                    // Add organizer first
                    if (organizerId != null) {
                        allUserIds.add(organizerId);
                    }

                    String[] subcollections = {"attendingUsers", "waitlistedUsers", "invitedUsers", "cancelledUsers"};
                    final int[] remainingFetches = {subcollections.length};

                    for (String subcollection : subcollections) {
                        db.collection("events").document(eventId).collection(subcollection)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    // Each document ID in the subcollection is a user ID
                                    for (var doc : querySnapshot.getDocuments()) {
                                        allUserIds.add(doc.getId());
                                    }

                                    // Check if all subcollections have been fetched
                                    remainingFetches[0]--;
                                    if (remainingFetches[0] == 0) {
                                        // All subcollections fetched, now send notifications
                                        sendNotificationToUsers(eventId, eventTitle, new ArrayList<>(allUserIds));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("AdminRepository", "Failed to fetch " + subcollection + " for event: " + eventId, e);
                                    remainingFetches[0]--;
                                    if (remainingFetches[0] == 0) {
                                        sendNotificationToUsers(eventId, eventTitle, new ArrayList<>(allUserIds));
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Failed to fetch event for deletion notification", e);
                });
    }

     /**
     * Helper method to send notifications to a list of users.
     * @param eventId eventId associated with notification.
     * @param eventTitle title of event associated with notification.
     * @param userIds user id's to send notification to.
     */
    private void sendNotificationToUsers(String eventId, String eventTitle, ArrayList<String> userIds) {
        if (userIds.isEmpty()) {
            Log.d("AdminRepository", "No users associated with event: " + eventId);
            return;
        }

        // Create the notification content
        Map<String, Object> notificationContent = new HashMap<>();
        notificationContent.put("eventTitle", eventTitle);
        notificationContent.put("notificationType", "This event has been deleted by an admin.");
        notificationContent.put("eventId", null);
        notificationContent.put("image", R.drawable.ic_radar);
        notificationContent.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());

        // Add to top-level notifications collection
        db.collection("notifications").add(notificationContent)
                .addOnSuccessListener(notificationRef -> {
                    // Fan out to each unique user
                    for (String userId : userIds) {
                        // Check if user has notifications enabled
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    if (Boolean.FALSE.equals(userDoc.getBoolean("notificationsEnabled"))) {
                                        Log.d("AdminRepository", "User " + userId + " has notifications disabled. Skipping.");
                                        return;
                                    }

                                    // Create user-specific stub
                                    Map<String, Object> userStub = new HashMap<>();
                                    userStub.put("readStatus", false);
                                    userStub.put("notificationRef", notificationRef);

                                    // Add to user's notification subcollection
                                    db.collection("users")
                                            .document(userId)
                                            .collection("notifications")
                                            .add(userStub)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("AdminRepository", "Event deletion notification sent to user: " + userId);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e("AdminRepository", "Failed to add notification to user " + userId, e);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("AdminRepository", "Failed to fetch user: " + userId, e);
                                });
                    }

                    Log.d("AdminRepository", "Event deletion notifications sent to " + userIds.size() + " users");
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Failed to create event deletion notification", e);
                });
    }

    /**
     * One time clean up function I used because I accidentally added a notification with a null image.
     */
    public void cleanupNotifications() {
        db.collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (!doc.contains("image") || doc.get("image") == null) {
                            doc.getReference().update("image", R.drawable.ic_radar)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d("AdminRepository", "Fixed notification: " + doc.getId()))
                                    .addOnFailureListener(e ->
                                            Log.e("AdminRepository", "Failed to fix: " + doc.getId(), e));
                        }
                    }
                });
    }

}
