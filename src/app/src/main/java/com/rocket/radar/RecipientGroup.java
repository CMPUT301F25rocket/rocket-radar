package com.rocket.radar;

/**
 * Represents the intended audience group for a notification.
 * While notifications are delivered to specific user subcollections, this enum helps
 * categorize the *intent* of the message (e.g., was it meant for everyone or just a subset?).
 * This is useful for analytics or for display purposes in an organizer's UI.
 * <p>
 * Author: Braden Woods
 */
public enum RecipientGroup {
    /**
     * The notification is intended for all attendees or potential attendees of an event.
     */
    ALL("all"),

    /**
     * The notification is intended only for users who have been selected to attend (e.g., lottery winners).
     */
    SELECTED("selected"),

    /**
     * The notification is intended for users whose attendance has been cancelled.
     */
    CANCELLED("cancelled");

    /**
     * The string value that is actually stored in the Firestore document.
     * Using a custom string value (e.g., lowercase) makes the database data cleaner
     * and independent of the Java enum constant name.
     */
    private final String value;

    /**
     * Private constructor to associate a string value with each enum constant.
     * @param value The lowercase string to store in Firestore.
     */
    RecipientGroup(String value) {
        this.value = value;
    }

    /**
     * Gets the string value to be stored in Firestore.
     * @return The lowercase string representation (e.g., "selected").
     */
    public String getValue() {
        return value;
    }

    /**
     * A helper method to safely convert a string from Firestore back into a RecipientGroup enum.
     * This is more robust than Enum.valueOf() for enums with custom string values.
     * Note: The version in the Notification class itself (getRecipientGroupEnum) is preferred
     *       for direct deserialization.
     *
     * @param s The string value from the Firestore document (e.g., "all", "selected").
     * @return The matching RecipientGroup, or a safe default (ALL) if no match is found.
     */
    public static RecipientGroup fromString(String s) {
        if (s == null) {
            return ALL; // Default if the value is missing.
        }
        // Loop through all enum constants to find one with a matching string value.
        for (RecipientGroup g : values()) {
            if (g.value.equalsIgnoreCase(s)) {
                return g;
            }
        }
        return ALL; // Return default if no match was found.
    }
}
