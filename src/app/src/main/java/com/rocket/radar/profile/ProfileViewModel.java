package com.rocket.radar.profile;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewModel extends ViewModel {

    private final static String TAG = "ProfileViewModel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final ProfileRepository profileRepository = new ProfileRepository(db);

    private final MutableLiveData<ProfileModel> profileLiveData = new MutableLiveData<>();
    public LiveData<ProfileModel> getProfileLiveData() {
        return this.profileLiveData;
    }

    public ProfileViewModel() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
                if (e != null) {
                    Log.e(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    ProfileModel profile = snapshot.toObject(ProfileModel.class);
                    profileLiveData.setValue(profile);
                } else {
                    Log.d(TAG, "Current data: null");
                }
            });
        }
    }


    public void updateProfile(ProfileModel profile) {
        profileRepository.writeProfile(profile, new ProfileRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Profile updated successfully: " + profile.getUid());
                // LiveData will be updated by the snapshot listener
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error updating profile " + profile.getUid(), e);
            }
        });
    }

    // This method is no longer the primary way to get profile data,
    // as the ViewModel now automatically listens for updates.
    // It can be kept for one-off fetches if needed elsewhere.
    public void getProfile(String uid) {
        profileRepository.readProfile(uid, new ProfileRepository.ReadCallback() {
            @Override
            public void onProfileLoaded(ProfileModel profile) {
                if (profile == null) {
                    Log.e(TAG, "No profile found for this UID ");
                    return;
                }
                Log.d(TAG, "Profile read successfully: " + profile.getUid());
                profileLiveData.postValue(profile);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error getting profile " + uid, e);
            }
        });
    }

    public void updateLastLogin(String uid) {
        profileRepository.updateLastLogin(uid);
    }
}
