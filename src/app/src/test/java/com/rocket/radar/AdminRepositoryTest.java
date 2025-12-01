package com.rocket.radar;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.rocket.radar.admin.AdminRepository;
import com.rocket.radar.profile.ProfileModel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//cite: the following script was adapted from Claude and ChatGPT, "How should I unit test my admin repository" 2025-12-01
/**
 * Unit tests for AdminRepository using Mockito to mock Firebase Firestore.
 * Tests cover user management, event management, and notification sending.
 */
@RunWith(RobolectricTestRunner.class)
public class AdminRepositoryTest {

    @Mock
    private FirebaseFirestore mockDb;

    @Mock
    private CollectionReference mockUsersCollection;

    @Mock
    private CollectionReference mockEventsCollection;

    @Mock
    private CollectionReference mockNotificationsCollection;

    @Mock
    private DocumentReference mockUserDoc;

    @Mock
    private DocumentReference mockEventDoc;

    @Mock
    private Task<QuerySnapshot> mockQueryTask;

    @Mock
    private Task<DocumentSnapshot> mockDocTask;

    @Mock
    private Task<Void> mockVoidTask;

    @Mock
    private QuerySnapshot mockQuerySnapshot;

    @Mock
    private DocumentSnapshot mockDocSnapshot;

    @Mock
    private QueryDocumentSnapshot mockQueryDocSnapshot;

    private AdminRepository adminRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        adminRepository = new AdminRepository(mockDb);
    }

    /**
     * Test that getAllUsers returns a populated list of ProfileModels.
     */
    @Test
    public void testGetAllUsers_ReturnsUsersList() {
        // Arrange: Create mock user data
        ProfileModel user1 = new ProfileModel();
        user1.setName("Alice");
        user1.setUid("uid1");

        ProfileModel user2 = new ProfileModel();
        user2.setName("Bob");
        user2.setUid("uid2");

        List<QueryDocumentSnapshot> mockDocs = Arrays.asList(mockQueryDocSnapshot, mockQueryDocSnapshot);

        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            when(mockQuerySnapshot.iterator()).thenReturn(mockDocs.iterator());
            when(mockQueryDocSnapshot.toObject(ProfileModel.class))
                    .thenReturn(user1)
                    .thenReturn(user2);
            when(mockQueryDocSnapshot.getId())
                    .thenReturn("uid1")
                    .thenReturn("uid2");
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // Act: Call getAllUsers
        final List<ProfileModel>[] result = new List[]{null};
        adminRepository.getAllUsers(profiles -> result[0] = profiles);

        // Assert: Verify correct users returned
        assertNotNull(result[0]);
        assertEquals(2, result[0].size());
        assertEquals("Alice", result[0].get(0).getName());
        assertEquals("Bob", result[0].get(1).getName());
    }

    /**
     * Test that getAllUsers returns empty list on failure.
     */
    @Test
    public void testGetAllUsers_OnFailure_ReturnsEmptyList() {
        // Arrange: Setup failure scenario
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(new Exception("Firestore error"));
            return mockQueryTask;
        });

        // Act: Call getAllUsers
        final List<ProfileModel>[] result = new List[]{null};
        adminRepository.getAllUsers(profiles -> result[0] = profiles);

        // Assert: Verify empty list returned
        assertNotNull(result[0]);
        assertTrue(result[0].isEmpty());
    }

    /**
     * Test that deleteUser successfully removes user and calls onSuccess.
     */
    @Test
    public void testDeleteUser_Success_CallsOnSuccess() {
        // Arrange: Setup user to delete
        ProfileModel user = new ProfileModel();
        user.setUid("testUid");
        user.setName("Test User");

        setupDeleteUserMocks();

        // Act: Delete the user
        final boolean[] successCalled = {false};
        adminRepository.deleteUser(user, new AdminRepository.DeleteUserCallback() {
            @Override
            public void onSuccess() {
                successCalled[0] = true;
            }

            @Override
            public void onError(Exception e) {
                fail("onError should not be called");
            }
        });

        // Assert: Verify success callback was called
        assertTrue(successCalled[0]);
    }

    /**
     * Test that deleteUser calls onError when Firestore fails.
     */
    @Test
    public void testDeleteUser_FirestoreFailure_CallsOnError() {
        // Arrange: Setup failure scenario
        ProfileModel user = new ProfileModel();
        user.setUid("testUid");

        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(new Exception("Firestore error"));
            return mockQueryTask;
        });

        // Act: Attempt to delete user
        final Exception[] capturedError = {null};
        adminRepository.deleteUser(user, new AdminRepository.DeleteUserCallback() {
            @Override
            public void onSuccess() {
                fail("onSuccess should not be called");
            }

            @Override
            public void onError(Exception e) {
                capturedError[0] = e;
            }
        });

        // Assert: Verify error callback was called
        assertNotNull(capturedError[0]);
        assertEquals("Firestore error", capturedError[0].getMessage());
    }

    /**
     * Test that updateUserRole successfully updates role to ADMIN.
     */
    @Test
    public void testUpdateUserRole_ToAdmin_Success() {
        // Arrange: Setup user role update
        ProfileModel user = new ProfileModel();
        user.setUid("testUid");
        user.setRole(ProfileModel.UserRole.ORGANIZER);

        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document("testUid")).thenReturn(mockUserDoc);
        when(mockUserDoc.update(eq("role"), any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        // Mock notification creation
        when(mockUserDoc.get()).thenReturn(mockDocTask);
        when(mockDocTask.addOnSuccessListener(any())).thenReturn(mockDocTask);
        when(mockDocTask.addOnFailureListener(any())).thenReturn(mockDocTask);

        // Act: Update role to ADMIN
        final boolean[] successCalled = {false};
        adminRepository.updateUserRole(user, ProfileModel.UserRole.ADMIN, new AdminRepository.updateCallback() {
            @Override
            public void onSuccess() {
                successCalled[0] = true;
            }

            @Override
            public void onError(Exception e) {
                fail("onError should not be called: " + e.getMessage());
            }
        });

        // Assert: Verify success
        assertTrue(successCalled[0]);
        verify(mockUserDoc).update("role", ProfileModel.UserRole.ADMIN);
    }


    /**
     * Test that deleteEvent returns error when event ID is null.
     */
    @Test
    public void testDeleteEvent_NullEventId_ReturnsError() {
        // Act: Attempt to delete with null ID
        final Exception[] capturedError = {null};
        adminRepository.deleteEvent(null, new AdminRepository.DeleteEventCallback() {
            @Override
            public void onSuccess() {
                fail("onSuccess should not be called");
            }

            @Override
            public void onError(Exception e) {
                capturedError[0] = e;
            }
        });

        // Assert: Verify error about null ID
        assertNotNull(capturedError[0]);
        assertTrue(capturedError[0] instanceof IllegalArgumentException);
        assertTrue(capturedError[0].getMessage().contains("cannot be null"));
    }

    /**
     * Test that removeUserFromAllEvents removes user from event subcollections.
     */
    @Test
    public void testRemoveUserFromAllEvents_RemovesUserFromSubcollections() {
        // Arrange: Setup mock events with user in subcollections
        String userId = "user123";

        CollectionReference mockSubcollection = mock(CollectionReference.class);
        DocumentReference mockSubDoc = mock(DocumentReference.class);

        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            List<DocumentSnapshot> docs = Arrays.asList(mockDocSnapshot);
            when(mockQuerySnapshot.getDocuments()).thenReturn(docs);
            when(mockDocSnapshot.getId()).thenReturn("event1");
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        when(mockEventsCollection.document("event1")).thenReturn(mockEventDoc);
        when(mockEventDoc.collection(anyString())).thenReturn(mockSubcollection);
        when(mockSubcollection.document(userId)).thenReturn(mockSubDoc);
        when(mockSubDoc.delete()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        // Act: Remove user from all events
        final boolean[] completeCalled = {false};
        adminRepository.removeUserFromAllEvents(userId, () -> completeCalled[0] = true, e -> fail("onError should not be called"));

        // Assert: Verify completion callback was called
        assertTrue(completeCalled[0]);
        verify(mockSubDoc, atLeast(1)).delete();
    }

    @Test
    public void testUpdateUserRole_SendsNotification() {
        // Arrange: setup user
        ProfileModel user = new ProfileModel();
        user.setUid("testUid");
        user.setName("Test User");
        user.setRole(ProfileModel.UserRole.ENTRANT);

        // Mock Firestore collections and documents
        CollectionReference mockUsersCollection = mock(CollectionReference.class);
        DocumentReference mockUserDoc = mock(DocumentReference.class);
        Task<Void> mockUpdateTask = mock(Task.class);
        Task<DocumentSnapshot> mockGetUserTask = mock(Task.class);
        DocumentSnapshot mockDocSnapshot = mock(DocumentSnapshot.class);

        CollectionReference mockNotificationsCollection = mock(CollectionReference.class);
        Task<DocumentReference> mockAddTask = mock(Task.class);
        DocumentReference mockNotificationRef = mock(DocumentReference.class);

        // Setup AdminRepository with mocked Firestore
        FirebaseFirestore mockDb = mock(FirebaseFirestore.class);
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockDb.collection("notifications")).thenReturn(mockNotificationsCollection);

        when(mockUsersCollection.document("testUid")).thenReturn(mockUserDoc);

        // Mock update role task
        when(mockUserDoc.update(eq("role"), any())).thenReturn(mockUpdateTask);
        when(mockUpdateTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockUpdateTask;
        });
        when(mockUpdateTask.addOnFailureListener(any())).thenReturn(mockUpdateTask);

        // Mock get user task (to check notifications enabled)
        when(mockUserDoc.get()).thenReturn(mockGetUserTask);
        when(mockGetUserTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockDocSnapshot.getBoolean("notificationsEnabled")).thenReturn(true);
            when(mockDocSnapshot.getString("name")).thenReturn("Test User");
            listener.onSuccess(mockDocSnapshot);
            return mockGetUserTask;
        });
        when(mockGetUserTask.addOnFailureListener(any())).thenReturn(mockGetUserTask);

        // Mock notifications add task
        when(mockNotificationsCollection.add(any(Map.class))).thenReturn(mockAddTask);
        when(mockAddTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentReference> listener = invocation.getArgument(0);
            listener.onSuccess(mockNotificationRef);
            return mockAddTask;
        });
        when(mockAddTask.addOnFailureListener(any())).thenReturn(mockAddTask);

        // Mock adding notification to user's subcollection
        CollectionReference mockUserNotifSub = mock(CollectionReference.class);
        when(mockUserDoc.collection("notifications")).thenReturn(mockUserNotifSub);
        Task<DocumentReference> mockUserAddTask = mock(Task.class);
        when(mockUserNotifSub.add(any(Map.class))).thenReturn(mockUserAddTask);
        when(mockUserAddTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentReference> listener = invocation.getArgument(0);
            listener.onSuccess(mockNotificationRef);
            return mockUserAddTask;
        });
        when(mockUserAddTask.addOnFailureListener(any())).thenReturn(mockUserAddTask);

        AdminRepository adminRepository = new AdminRepository(mockDb);

        // Act: call updateUserRole
        adminRepository.updateUserRole(user, ProfileModel.UserRole.ORGANIZER, new AdminRepository.updateCallback() {
            @Override
            public void onSuccess() {
                // Success
            }

            @Override
            public void onError(Exception e) {
                fail("Should not fail: " + e.getMessage());
            }
        });

        // Assert: verify that notifications collection was accessed
        verify(mockDb, atLeastOnce()).collection("notifications");
        verify(mockUserDoc, atLeastOnce()).collection("notifications");
    }


    /**
     * Test that deleteEvent does not proceed if event doesn't exist.
     */
    @Test
    public void testDeleteEvent_NonExistentEvent_ReturnsError() {
        // Arrange: Setup non-existent event
        String eventId = "nonExistentEvent";

        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(eventId)).thenReturn(mockEventDoc);
        when(mockEventDoc.get()).thenReturn(mockDocTask);
        when(mockDocTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockDocSnapshot.exists()).thenReturn(false);
            listener.onSuccess(mockDocSnapshot);
            return mockDocTask;
        });
        when(mockDocTask.addOnFailureListener(any())).thenReturn(mockDocTask);

        // Act: Attempt to delete non-existent event
        final Exception[] capturedError = {null};
        adminRepository.deleteEvent(eventId, new AdminRepository.DeleteEventCallback() {
            @Override
            public void onSuccess() {
                fail("onSuccess should not be called");
            }

            @Override
            public void onError(Exception e) {
                capturedError[0] = e;
            }
        });

        // Assert: Verify error about event not found
        assertNotNull(capturedError[0]);
        assertTrue(capturedError[0].getMessage().contains("Event not found"));
    }

    // Helper methods to setup complex mock scenarios

    private void setupDeleteUserMocks() {
        // Mock removeUserFromAllEvents
        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            when(mockQuerySnapshot.getDocuments()).thenReturn(new ArrayList<>());
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // Mock notification deletion
        CollectionReference mockNotifCollection = mock(CollectionReference.class);
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDoc);
        when(mockUserDoc.collection("notifications")).thenReturn(mockNotifCollection);
        when(mockNotifCollection.get()).thenReturn(mockQueryTask);

        // Mock user document deletion
        when(mockUserDoc.delete()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);
    }

    private void setupUpdateRoleWithEventDeletionMocks() {
        // Mock event fetching and deletion
        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(anyString())).thenReturn(mockEventDoc);
        when(mockEventDoc.get()).thenReturn(mockDocTask);
        when(mockDocTask.addOnSuccessListener(any())).thenReturn(mockDocTask);
        when(mockDocTask.addOnFailureListener(any())).thenReturn(mockDocTask);
        when(mockEventDoc.delete()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);

        // Mock user collection and notifications
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDoc);
        when(mockUsersCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        when(mockUserDoc.update(eq("role"), any())).thenReturn(mockVoidTask);
        when(mockUserDoc.get()).thenReturn(mockDocTask);

        // Mock notifications
        when(mockDb.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.get()).thenReturn(mockQueryTask);
    }

    private void setupDeleteEventMocks(String eventId) {
        // Mock event document fetch
        when(mockDb.collection("events")).thenReturn(mockEventsCollection);
        when(mockEventsCollection.document(eventId)).thenReturn(mockEventDoc);
        when(mockEventDoc.get()).thenReturn(mockDocTask);
        when(mockDocTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<DocumentSnapshot> listener = invocation.getArgument(0);
            when(mockDocSnapshot.exists()).thenReturn(true);
            listener.onSuccess(mockDocSnapshot);
            return mockDocTask;
        });
        when(mockDocTask.addOnFailureListener(any())).thenReturn(mockDocTask);

        // Mock users collection for removal
        when(mockDb.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.get()).thenReturn(mockQueryTask);
        when(mockQueryTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<QuerySnapshot> listener = invocation.getArgument(0);
            when(mockQuerySnapshot.getDocuments()).thenReturn(new ArrayList<>());
            listener.onSuccess(mockQuerySnapshot);
            return mockQueryTask;
        });
        when(mockQueryTask.addOnFailureListener(any())).thenReturn(mockQueryTask);

        // Mock notifications collection
        when(mockDb.collection("notifications")).thenReturn(mockNotificationsCollection);
        when(mockNotificationsCollection.get()).thenReturn(mockQueryTask);

        // Mock event deletion
        when(mockEventDoc.delete()).thenReturn(mockVoidTask);
        when(mockVoidTask.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return mockVoidTask;
        });
        when(mockVoidTask.addOnFailureListener(any())).thenReturn(mockVoidTask);
    }
}