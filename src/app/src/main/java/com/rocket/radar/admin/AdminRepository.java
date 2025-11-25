package com.rocket.radar.admin;

import android.util.Log;

import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.rocket.radar.profile.ProfileModel;

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

    public void deleteUser(ProfileModel profile, DeleteCallback callback) {
        String uid = profile.getUid();

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
    }

    public interface DeleteCallback {
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

        // Get a reference to the event document
        db.collection("events").document(eventId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // Optional: delete all subcollections if needed
                        deleteEventSubcollections(eventId, () -> {
                            // Delete the main event document
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

    public interface DeleteEventCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface OnErrorCallback {
        void onError(Exception e);
    }

}
