package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.profile.ProfileModel;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class ProfileTests {

    private ProfileModel profile;

    @Before
    public void setUp() {
        profile = new ProfileModel("testUser", "Test User", "test@example.com", "123-456-7890", Timestamp.now(), true, true, ProfileModel.UserRole.ORGANIZER);
    }

    @Test
    public void testDefaultConstructor() {
        ProfileModel p = new ProfileModel();
        // Firestore style constructor should leave things null or default
        assertNotNull(p.getOnInvitedEventIds()); // getter initializes list
        assertNotNull(p.getAttendingEventIds());
        assertNotNull(p.getOnMyEventIds());
        assertNotNull(p.getOnWaitlistEventIds());
        
        // Boolean defaults via getters
        assertTrue(p.isNotificationsEnabled()); // defaults to true if null
        assertFalse(p.isGeolocationEnabled()); // defaults to false if null
    }

    @Test
    public void testGetSetLastKnownLocation() {
        GeoPoint gp = new GeoPoint(10.0, 20.0);
        profile.setLastKnownLocation(gp);
        assertEquals(gp, profile.getLastKnownLocation());
    }

    @Test
    public void testUserRoles() {
        profile.setRole(ProfileModel.UserRole.ADMIN);
        assertEquals(ProfileModel.UserRole.ADMIN, profile.getRole());
        
        profile.setRole(ProfileModel.UserRole.ENTRANT);
        assertEquals(ProfileModel.UserRole.ENTRANT, profile.getRole());
        
        profile.setRole(null);
        // Should default to ORGANIZER or keep current? The setter does nothing if null.
        // The getter defaults to ORGANIZER if the string is null.
        // If we pass null to setter, the field isn't changed.
        // Let's test getter default logic by creating a fresh profile
        ProfileModel empty = new ProfileModel();
        assertEquals(ProfileModel.UserRole.ORGANIZER, empty.getRole());
    }

    @Test
    public void testBasicGettersSetters() {
        profile.setName("New Name");
        assertEquals("New Name", profile.getName());
        
        profile.setEmail("new@test.com");
        assertEquals("new@test.com", profile.getEmail());
        
        profile.setPhoneNumber("987");
        assertEquals("987", profile.getPhoneNumber());
        
        profile.setUid("uid2");
        assertEquals("uid2", profile.getUid());
    }
}
