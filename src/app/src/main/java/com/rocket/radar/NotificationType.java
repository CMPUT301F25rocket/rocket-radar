package com.rocket.radar;

/**
 * Defines the specific category or purpose of a notification.
 * This enum is crucial for the UI layer to determine what action to take when a
 * notification is clicked (e.g., show an RSVP dialog vs. a generic info dialog).
 * <p>
 * The names of these enums are stored as strings in Firestore, so they should be
 * descriptive and stable.
 * <p>
 * Author: Braden Woods
 */
public enum NotificationType {
    /**
     * For general announcements about an event that are sent to all attendees.
     * Example: "The event location has changed."
     */
    EVENT_DETAILS,

    /**
     * For notifications related to a user's status on a waitlist.
     * Example: "A spot has opened up! You have 24 hours to claim it."
     */
    WAITLIST_STATUS,

    /**
     * For notifications informing a user they have been successfully selected from a lottery or waitlist.
     * This is typically a high-priority message that may trigger a specific action, like an RSVP.
     * Example: "Congratulations! You're in for the BBQ event."
     */
    SELECTED_ENTRANTS,

    /**
     * For notifications informing a user that their entry or ticket has been cancelled.
     * Example: "Your registration for the event has been cancelled."
     */
    CANCELLED_ENTRANTS,

    /**
     * A fallback or default type for notifications that don't fit other categories or
     * in case the type string from Firestore is unrecognized. This prevents the app from crashing.
     */
    GENERIC_MESSAGE;

    /**
     * A helper method to safely parse a string into its corresponding NotificationType enum.
     * This is useful when reading data that might not be perfectly clean.
     * Note: The version in the Notification class itself (getTypeEnum) is preferred for direct
     *       deserialization as it's attached to the model object.
     *
     * @param s The string to parse (e.g., "SELECTED_ENTRANTS").
     * @return The matching NotificationType, or GENERIC_MESSAGE if no match is found.
     */
    public static NotificationType fromString(String s) {
        try {
            // valueOf() is case-sensitive, so converting to uppercase makes this more robust.
            return valueOf(s.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            // If the string is null or doesn't match any enum constant, return the safe default.
            return GENERIC_MESSAGE;
        }
    }
}
