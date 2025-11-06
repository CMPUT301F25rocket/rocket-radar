package com.rocket.radar.profile;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

//cite: general design was based on https://developer.android.com/topic/architecture, to separate the ui layer in the architecture from the data (repository)
public class ProfileViewModel extends ViewModel {

    private final static String TAG = "ProfileViewModel";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final ProfileRepository profileRepository = new ProfileRepository(db);
    private ListenerRegistration profileListenerRegistration; // To manage listener lifecycle

    private final MutableLiveData<ProfileModel> profileLiveData = new MutableLiveData<>();
    public LiveData<ProfileModel> getProfileLiveData() {
        return this.profileLiveData;
    }

    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getDeleteSuccess() { return deleteSuccess; }

    public ProfileViewModel() {
        // The listener is no longer set in the constructor.
        // It will be set explicitly when a user is signed in.
    }

    /**
     * Sets up the Firestore snapshot listener for the given user ID.
     * This method ensures the ViewModel listens to real-time updates for the specified user's profile.
     * If a listener is already active, it is first removed to prevent multiple listeners.
     *
     * @param uid The unique ID of the user to listen for.
     */
    public void setUserIdForProfileListener(String uid) {
        if (uid == null) {
            Log.w(TAG, "Cannot set listener for a null UID.");
            return;
        }

        // Remove any existing listener to avoid leaks or duplicate listeners
        if (profileListenerRegistration != null) {
            profileListenerRegistration.remove();
        }

        profileListenerRegistration = db.collection("users").document(uid).addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for UID: " + uid, e);
                profileLiveData.setValue(null); // Clear data on error
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                ProfileModel profile = snapshot.toObject(ProfileModel.class);
                if (profile != null) {
                    profile.setUid(snapshot.getId()); // Ensure UID is set
                    profileLiveData.setValue(profile);
                }
            } else {
                Log.d(TAG, "Profile data is null for UID: " + uid);
                profileLiveData.setValue(null); // User document doesn't exist yet
            }
        });
    }
    public void deleteProfile(ProfileModel profile) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "No authenticated user to delete.");
            deleteSuccess.postValue(false);
            return;
        }

        profileRepository.deleteAccount(user, profile, new ProfileRepository.WriteCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Profile deleted successfully.");
                deleteSuccess.postValue(true);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error deleting profile", e);
                deleteSuccess.postValue(false);
            }
        });
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
        if (uid == null) {
            Log.e(TAG, "Cannot update last login for null UID.");
            return;
        }
        profileRepository.updateLastLogin(uid);
    }

    /**
     * Clean up the listener when the ViewModel is cleared.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (profileListenerRegistration != null) {
            profileListenerRegistration.remove();
            Log.d(TAG, "Profile listener removed.");
        }
    }
}
