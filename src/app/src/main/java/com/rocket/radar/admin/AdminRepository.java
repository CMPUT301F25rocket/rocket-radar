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
}
