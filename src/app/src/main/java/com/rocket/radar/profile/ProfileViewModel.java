package com.rocket.radar.profile;

import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileViewModel extends ViewModel {

    private final static String TAG = "ProfileViewModel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private final ProfileRepository profileRepository = new ProfileRepository(db);

    private final MutableLiveData<ProfileModel> profileLiveData = new MutableLiveData<>(); // null because being loaded async from the db
    public LiveData<ProfileModel> getProfileLiveData() {
        return this.profileLiveData;
    }

    public void updateProfile(ProfileModel profile) {
        profileRepository.writeProfile(profile, new ProfileRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Profile updated successfully: " + profile.getUid());
                profileLiveData.postValue(profile);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error updating profile " + profile.getUid(), e);
            }
        });
    }

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
