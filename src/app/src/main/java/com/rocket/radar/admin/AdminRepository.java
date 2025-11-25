package com.rocket.radar.admin;

import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rocket.radar.notifications.Notification;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.events.Event;

import java.util.ArrayList;
import java.util.List;

public class AdminRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

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

    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }

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
    private void removeUserFromAllEvents(String uid, Runnable onComplete, OnErrorCallback onError) {
        db.collection("events").get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Event event = doc.toObject(Event.class);

                        boolean wasModified = false;

                        // Remove user from all status lists
                        if (event.getEventAttendingIds().remove(uid)) {
                            wasModified = true;
                        }
                        if (event.getEventWaitlistIds().remove(uid)) {
                            wasModified = true;
                        }
                        if (event.getEventInvitedIds().remove(uid)) {
                            wasModified = true;
                        }
                        if (event.getEventCancelledIds().remove(uid)) {
                            wasModified = true;
                        }

                        // Update the event if any lists were modified
                        if (wasModified) {
                            doc.getReference().update(
                                    "eventAttendingIds", event.getEventAttendingIds(),
                                    "eventWaitlistIds", event.getEventWaitlistIds(),
                                    "eventInvitedIds", event.getEventInvitedIds(),
                                    "eventCancelledIds", event.getEventCancelledIds()
                            ).addOnFailureListener(e -> {
                                Log.e("AdminRepository", "Failed to update event " + doc.getId(), e);
                                onError.onError(e);
                            });
                        }
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminRepository", "Failed to query events for user removal", e);
                    onError.onError(e);
                });
    }

    public interface DeleteUserCallback {
        void onSuccess();
        void onError(Exception e);
    }


    public void updateUserRole(ProfileModel profile, ProfileModel.UserRole role, updateCallback callback) {
        String uid = profile.getUid();
        db.collection("users")
                .document(uid)
                .update("role", role)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess();
                })
                .addOnFailureListener( e -> {
                    Log.e("AdminRepository", "Failed to fetch notifications", e);
                    callback.onError(e);
                });
    }

    public interface updateCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public void deleteEvent(String eventId, DeleteEventCallback callback) {
        if (eventId == null || eventId.isEmpty()) {
            callback.onError(new IllegalArgumentException("Event ID cannot be null or empty"));
            return;
        }

        removeEventFromAllUsers(eventId, () -> {
            removeNotificationsAboutEvent(eventId, () -> {
                db.collection("events").document(eventId)
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
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
                            } else {
                                callback.onError(new Exception("Event not found: " + eventId));
                            }
                        })
                        .addOnFailureListener(callback::onError);
            }, callback::onError);
        }, callback::onError);
    }

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
     * Scans all users and removes the event ID from their onMyEventIds list.
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

    public interface DeleteEventCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnErrorCallback {
        void onError(Exception e);
    }

}
