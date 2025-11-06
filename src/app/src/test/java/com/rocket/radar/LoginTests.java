package com.rocket.radar;

import static org.junit.Assert.assertNotNull;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.Test;

public class LoginTests {
    FirebaseAuth mockAuth = org.mockito.Mockito.mock(FirebaseAuth.class);
    @Test
    public void testFirebaseAuthInitialization() {
        assertNotNull("FirebaseAuth instance should not be null", mockAuth);
    }
    @Test
    public void testStartScanningFragmentCreation() {
        com.rocket.radar.login.LoginStartScanningFragment fragment = new com.rocket.radar.login.LoginStartScanningFragment();
        assertNotNull("LoginStartScanningFragment instance should not be null", fragment);
    }
}




