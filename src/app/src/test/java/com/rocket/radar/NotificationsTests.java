package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rocket.radar.notifications.Notification;

import org.junit.Test;

import java.util.Date;

public class NotificationsTests {

    @Test
    public void testNotificationConstructorAndGetters() {
        // Since the constructor is empty (Firebase style), we test setters and getters manually
        // or if we had a full constructor we would test that.
        // Here we primarily rely on field access via getters/setters for the UI logic.
        
        Notification notification = new Notification();
        assertNull(notification.getNotificationId());
        assertNull(notification.getEventTitle());
        
        notification.setReadStatus(true);
        assertTrue(notification.isReadStatus());
        
        notification.setReadStatus(false);
        assertFalse(notification.isReadStatus());
    }

    @Test
    public void testUserNotificationIdLinking() {
        Notification notification = new Notification();
        String userId = "user123";
        String userNotifId = "notif_u_123";
        
        notification.setUserNotificationId(userNotifId);
        
        assertEquals(userNotifId, notification.getUserNotificationId());
    }
}
