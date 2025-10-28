package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Local unit tests for the Notification model class.
 * This test verifies the logic of the model itself, especially its ability
 * to safely handle potentially malformed data from Firestore.
 */
public class NotificationTest {

    @Test
    public void getTypeEnum_returnsCorrectEnum_forValidTypeString() {
        // GIVEN: A Notification object with a valid type string
        Notification notification = new Notification();
        notification.setType("SELECTED_ENTRANTS");

        // WHEN: We call the convenience method to get the enum
        NotificationType result = notification.getTypeEnum();

        // THEN: The correct enum constant should be returned
        assertEquals(NotificationType.SELECTED_ENTRANTS, result);
    }

    @Test
    public void getTypeEnum_returnsGenericMessage_forInvalidTypeString() {
        // GIVEN: A Notification object with a misspelled or unknown type string
        Notification notification = new Notification();
        notification.setType("THIS_IS_NOT_A_VALID_TYPE");

        // WHEN: We call the convenience method
        NotificationType result = notification.getTypeEnum();

        // THEN: It should return the safe default (GENERIC_MESSAGE) to prevent a crash
        assertEquals(NotificationType.GENERIC_MESSAGE, result);
    }

    @Test
    public void getTypeEnum_returnsGenericMessage_forNullTypeString() {
        // GIVEN: A Notification object where the type field is null (missing from Firestore)
        Notification notification = new Notification();
        notification.setType(null);

        // WHEN: We call the convenience method
        NotificationType result = notification.getTypeEnum();

        // THEN: It should return the safe default (GENERIC_MESSAGE) to prevent a crash
        assertEquals(NotificationType.GENERIC_MESSAGE, result);
    }

    @Test
    public void constructor_setsAllFieldsCorrectly() {
        // GIVEN: We use the convenience constructor to create a new notification
        String title = "Event Update";
        String description = "The location has changed.";
        String eventId = "event_456";
        String type = NotificationType.EVENT_DETAILS.name();
        String recipients = RecipientGroup.ALL.getValue();
        String senderId = "organizer_789";
        String imageUrl = "http://example.com/image.png";

        // WHEN: The Notification object is created
        Notification notification = new Notification(title, description, eventId, type, recipients, senderId, imageUrl);

        // THEN: All fields should be correctly assigned
        assertNotNull(notification);
        assertEquals(title, notification.getTitle());
        assertEquals(description, notification.getDescription());
        assertEquals(eventId, notification.getEventId());
        assertEquals(type, notification.getType());
        assertEquals(recipients, notification.getRecipients());
        assertEquals(senderId, notification.getSenderId());
        assertEquals(imageUrl, notification.getImageUrl());
    }
}
