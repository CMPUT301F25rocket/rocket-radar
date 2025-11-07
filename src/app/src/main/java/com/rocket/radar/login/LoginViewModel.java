package com.rocket.radar.login;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileViewModel;

/** ViewModel to manage user login and profile initialization. */
public class LoginViewModel extends AndroidViewModel {
    private static final String TAG = "LoginViewModel";

    private final FirebaseAuth mAuth;
    private final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isNewUserLiveData = new MutableLiveData<>();
    private final ProfileViewModel profileViewModel;

    /** Constructor initializes FirebaseAuth and ProfileViewModel. */
    public LoginViewModel(@NonNull Application application) {
        super(application);
        mAuth = FirebaseAuth.getInstance();
        // use your existing ViewModel to manage Firestore profile data
        profileViewModel = new ViewModelProvider.AndroidViewModelFactory(application)
                .create(ProfileViewModel.class);
    }
    /** LiveData for observing the current Firebase user. */
    public LiveData<FirebaseUser> getUserLiveData() {
        return userLiveData;
    }

    /** LiveData for observing if the user is new. */
    public LiveData<Boolean> getIsNewUserLiveData() {
        return isNewUserLiveData;
    }

    /** Checks if a user is signed in; if not, signs them in anonymously. */
    public void checkOrSignIn() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signInAnonymously();
        } else {
            userLiveData.setValue(currentUser);
            handleUserProfile(currentUser);
        }
    }

    /** Signs in the user anonymously and initializes their profile if needed. */
    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        boolean isNew = task.getResult().getAdditionalUserInfo().isNewUser();
                        if (isNew) {
                            isNewUserLiveData.postValue(true);
                        } else {
                            isNewUserLiveData.postValue(false);
                        }
                        userLiveData.setValue(user);
                        handleUserProfile(user);
                    } else {
                        Log.w(TAG, "Anonymous sign-in failed", task.getException());
                        userLiveData.setValue(null);
                    }
                });
    }

    /** Checks if the user's profile exists; if not, creates a default profile. */
    private void handleUserProfile(FirebaseUser user) {
        if (user == null) return;
        String uid = user.getUid();

        ProfileModel profile = profileViewModel.getProfileLiveData().getValue();
        if (profile == null) {
            Log.d(TAG, "No profile found for UID: " + uid + " — creating default one.");
            ProfileModel defaultProfile = new ProfileModel(
                    uid,
                    "Anonymous User",
                    "",
                    "",
                    null,
                    true,
                    true,
                    false
            );
            profileViewModel.updateProfile(defaultProfile);
            isNewUserLiveData.postValue(true);
        } else {
            Log.d(TAG, "Profile exists for user: " + profile.getUid());
            isNewUserLiveData.postValue(false);
        }
    }
}
