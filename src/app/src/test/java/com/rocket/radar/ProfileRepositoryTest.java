package com.rocket.radar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rocket.radar.admin.AdminRepository;
import com.rocket.radar.profile.ProfileModel;
import com.rocket.radar.profile.ProfileRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Map;

//cite: this script was created by ChatGPT, "How should I unit test my profile repository?" 2025-12-01
/**
 * This testing script tests the profile repository using mock unit tests.
 */
@RunWith(RobolectricTestRunner.class)
public class ProfileRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockUsersCollection;

    @Mock
    private DocumentReference mockUserDoc;

    @Mock
    private DocumentSnapshot mockSnapshot;

    @Mock
    private Task<DocumentSnapshot> mockGetTask;

    @Mock
    private Task<Void> mockVoidTask;

    @Mock
    private FirebaseUser mockFirebaseUser;

    @Mock
    private AdminRepository mockAdminRepository;

    private ProfileRepository profileRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock collection and document references
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(any())).thenReturn(mockUserDoc);
        when(mockUserDoc.get()).thenReturn(mockGetTask);

        profileRepository = new ProfileRepository(mockDb, mockAdminRepository);
    }


    @Test
    public void testReadProfile_Success() {
        String uid = "user123";
        ProfileModel profile = new ProfileModel();
        profile.setUid(uid);

        when(mockGetTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockSnapshot.toObject(ProfileModel.class)).thenReturn(profile);
            listener.onSuccess(mockSnapshot);
            return mockGetTask;
        });
        when(mockGetTask.addOnFailureListener(any())).thenReturn(mockGetTask);

        profileRepository.readProfile(uid, new ProfileRepository.ReadCallback() {
            @Override
            public void onProfileLoaded(ProfileModel loadedProfile) {
                assertEquals(uid, loadedProfile.getUid());
            }

            @Override
            public void onError(Exception e) {
                fail("Should not fail");
            }
        });
    }

    @Test
    public void testWriteProfile_Success() {
        ProfileModel profile = new ProfileModel();
        profile.setUid("user123");
        profile.setName("John Doe");

        when(mockUserDoc.set(any(Map.class), any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, OnSuccessListener.class).onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        profileRepository.writeProfile(profile, new ProfileRepository.WriteCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(Exception e) {
                fail("Should not fail");
            }
        });
    }

    @Test
    public void testAddEventIdToWaitlist_Success() {
        String uid = "user123";
        String eventId = "event123";

        when(mockUserDoc.update(eq("onWaitlistEventIds"), any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, OnSuccessListener.class).onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        profileRepository.addEventIdToWaitlist(uid, eventId, new ProfileRepository.WriteCallback() {
            @Override
            public void onSuccess() {}

            @Override
            public void onError(Exception e) {
                fail("Should not fail");
            }
        });
    }
}
