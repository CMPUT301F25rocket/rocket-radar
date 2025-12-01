package com.rocket.radar.events;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.Exclude; // CORRECT: Using the Firestore Exclude
import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.eventmanagement.Time;
import com.rocket.radar.uml.UmlAssociate;

import java.io.Serializable;

/**
 * Represents a single event in the application.
 *
 * <p>This class serves as a data model for event information, including details about the event,
 * scheduling, registration, and user participation. It is designed to be serialized for passing between
 * Android components (e.g., Fragments) and is compatible with Google Firestore for
 * database operations. The {@link Serializable} interface allows Event objects to be
 * passed in Bundles.</p>
 * <p>A {@link Builder} class is provided for flexible object creation.</p>
 */
public class Event implements Serializable {
    // This is the fixed list of categories that
    public static final List<String> allEventCategories = new ArrayList<>();
    static {
        allEventCategories.add("Sport");
        allEventCategories.add("Food");
        allEventCategories.add("Music");
        allEventCategories.add("Social");
        allEventCategories.add("Business");
        allEventCategories.add("Workshop");
        allEventCategories.add("Art");
    }


    // Primary event details
    private String eventId; // ADD THIS FIELD
    String eventTitle;
    String tagline;
    String description;
    SortedSet<String> categories;
    private Date eventStartDate;
    private Optional<Date> eventEndDate;
    private Time eventStartTime;
    private Time eventEndTime;
    private Date registrationStartDate;
    private Date selectionStartDate;
    private Date finalSelectionDate;
    private Optional<Integer> waitlistCapacity;
    private boolean requireLocation;
    private int eventCapacity;
    private String organizerId;

    private String eventLocationName;

    // Since GeoPoint is not Serializable, we might need to handle it carefully or exclude it from serialization
    // if we pass Event objects via Intent/Bundle. However, Firestore handles GeoPoint fine.
    // For Serializable, we can't use GeoPoint directly if we want to pass it in Bundle using Serializable.
    // But Event implements Serializable.
    // To avoid crashes when passing Event in Bundle, we should probably mark GeoPoint as transient
    // and handle it manually, or just store lat/long as doubles.
    // However, the prompt asks to save to Firebase as well. Firestore likes GeoPoint.
    // Let's store separate lat/lng for serialization safety or create a custom DTO.
    // Or we can make a transient GeoPoint and persistent double lat/lng.
    // ACTUALLY: GeoPoint IS NOT Serializable.
    // So if we put Event in a bundle, it will crash if GeoPoint is a field.
    // We should store lat/lng and helper method to get/set GeoPoint.

    private Double locationLatitude;
    private Double locationLongitude;


    private Blob bannerImageBlob;

    // WARN: DO NOT REMOVE TRANSIENT. WE WILL CONSUME OUR FIRESTORE USAGE FAST (maybe).
    // We don't want this one serialized.
    private transient Bitmap bannerImage;

    /**
     * A list of user IDs for those on the waitlist for the event.
     * These users may be moved to the attending list if spots become available.
     */
    ArrayList<String> eventWaitlistIds;
    /**
     * A list of user IDs for those who have been invited to the event.
     * This is typically used in specific event types where invitations are sent out.
     */
    ArrayList<String> eventInvitedIds;
    /**
     * A list of user IDs for those who are confirmed to be attending the event.
     */
    ArrayList<String> eventAttendingIds;
    /**
     * A list of user IDs for those who have cancelled their attendance or registration.
     */
    ArrayList<String> eventCancelledIds;

    int image;

    /**
     * Default constructor required for Firestore data mapping.
     * Initializes transient fields to null.
     */
    public Event() {
        // Default constructor required
        bannerImage = null;
    }

    /**
     * Constructs a new Event with essential details.
     * A unique event ID is automatically generated.
     *
     * @param eventTitle The title of the event.
     * @param date The start date of the event.
     * @param tagline A short, catchy phrase for the event.
     * @param description A detailed description of the event.
     * @param image A local drawable resource ID for a placeholder image.
     */
    public Event(String eventTitle, Date date, String tagline, String description, int image) {
        this.eventId = UUID.randomUUID().toString(); // Generate a unique ID
        this.eventTitle = eventTitle;
        this.eventStartDate = date; // Assuming date is in "YYYY-MM-DD" format
        this.tagline = tagline;
        this.image = image;
        this.description = description;
        bannerImage = null;
    }

    // Standard getters

    /**
     * Resizes the provided bitmap to the specific width and height.
     *
     * @param image  The original bitmap image.
     * @param width  The target width.
     * @param height The target height.
     * @return A new Bitmap scaled to the specified dimensions.
     */
    public static Bitmap resizeBanner(Bitmap image, int width, int height) {
        // Using 'true' for filter improves quality when scaling down
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    /**
     * Gets the title of the event.
     * @return The event title as a String.
     */
    public String getEventTitle() { return eventTitle; }
    /**
     * Gets the date of the event.
     * @return The event date as a String.
     */
    /**
     * Gets the start date of the event.
     *
     * @return The {@link Date} object representing the event's start date, or null if not set.
     */
    public Date getEventStartDate() { return eventStartDate != null ? eventStartDate : null;}
    /**
     * Formats the event start date for display purposes.
     * Returns the date in format "DD\nMMM" where MMM is a three-letter uppercase abbreviation for the month.
     * @return Formatted date string with day and month on separate lines, or empty string if date is not set.
     */
    public String getFormattedDate() {
        // returns the date in format DD\nMMM where MMM three letter capital abbreviation for the month
        if (eventStartDate == null) return "";
        LocalDate localDate = eventStartDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        String day = localDate.getDayOfMonth() + "";
        // capital letters for 3 letter month abbrev
        String month = localDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase();
        return day + "\n" + month;
    }

    /**
     * Gets the tagline of the event.
     *
     * @return The event tagline as a String.
     */
    public String getTagline() { return tagline; }
    /**
     * Gets the unique identifier of the event.
     * @return The event ID as a String.
     */
    public String getEventId() { return eventId; }

    /**
     * Sets the unique identifier for the event. This is primarily used by Firestore
     * during deserialization.
     * @param eventId The unique identifier string.
     */
    public void setEventId(String eventId) { this.eventId = eventId; }



    /**
     * Gets the maximum capacity of the event.
     *
     * @return The event's capacity.
     */
    public int getEventCapacity() {
        return eventCapacity;
    }

    /**
     * Sets the maximum capacity of the event.
     *
     * @param eventCapacity The event's capacity.
     */
    public void setEventCapacity(int eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

    /**
     * Checks if location tracking is required for this event.
     *
     * @return true if location is required, false otherwise.
     */
    public boolean isRequireLocation() {
        return requireLocation;
    }

    /**
     * Sets whether location tracking is required for this event.
     *
     * @param requireLocation true to require location, false otherwise.
     */
    public void setRequireLocation(boolean requireLocation) {
        this.requireLocation = requireLocation;
    }

    /**
     * Gets the capacity of the waitlist.
     *
     * @return An {@link Integer} representing the waitlist capacity, or null if not set.
     */
    public Integer getWaitlistCapacity() {
        return waitlistCapacity != null ? waitlistCapacity.orElse(null) : null;
    }

    /**
     * Sets the capacity of the waitlist.
     *
     * @param waitlistCapacity The waitlist capacity. Can be null.
     */
    public void setWaitlistCapacity(Integer waitlistCapacity) {
        this.waitlistCapacity = Optional.ofNullable(waitlistCapacity);
    }

    /**
     * Gets the final date for selections to be made.
     *
     * @return The final selection {@link Date}.
     */
    public Date getFinalSelectionDate() {
        return finalSelectionDate;
    }

    /**
     * Sets the final date for selections.
     *
     * @param finalSelectionDate The final selection {@link Date}.
     */
    public void setFinalSelectionDate(Date finalSelectionDate) {
        this.finalSelectionDate = finalSelectionDate;
    }


    /**
     * Gets the start date of the selection period.
     *
     * @return The selection start {@link Date}.
     */
    public Date getSelectionStartDate() {
        return selectionStartDate;
    }

    /**
     * Sets the start date of the selection period.
     *
     * @param selectionStartDate The selection start {@link Date}.
     */
    public void setSelectionStartDate(Date selectionStartDate) {
        this.selectionStartDate = selectionStartDate;
    }


    /**
     * Gets the start date of the registration period.
     *
     * @return The registration start {@link Date}.
     */
    public Date getRegistrationStartDate() {
        return registrationStartDate;
    }

    /**
     * Sets the start date of the registration period.
     *
     * @param registrationStartDate The registration start {@link Date}.
     */
    public void setRegistrationStartDate(Date registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    /**
     * Gets the end time of the event.
     *
     * @return The event end {@link Time}.
     */
    public Time getEventEndTime() {
        return eventEndTime;
    }

    /**
     * Sets the end time of the event.
     *
     * @param eventEndTime The event end {@link Time}.
     */
    public void setEventEndTime(Time eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    /**
     * Gets the start time of the event.
     *
     * @return The event start {@link Time}.
     */
    public Time getEventStartTime() {
        return eventStartTime;
    }

    /**
     * Sets the start time of the event.
     *
     * @param eventStartTime The event start {@link Time}.
     */
    public void setEventStartTime(Time eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    /**
     * Gets the end date of the event.
     *
     * @return The event end {@link Date}, or null if not set.
     */
    public Date getEventEndDate() {
        return eventEndDate != null ? eventEndDate.orElse(null) : null;
    }

    /**
     * Sets the end date of the event.
     *
     * @param eventEndDate The event end {@link Date}. Can be null.
     */
    public void setEventEndDate(Date eventEndDate) {
        this.eventEndDate = Optional.ofNullable(eventEndDate);
    }


    /**
     * Sets the ID of the user who organized this event.
     * @param organizerId The organizer's User ID.
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Gets the ID of the user who organized this event.
     * @return The organizer's User ID.
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets the start date of the event.
     *
     * @param eventStartDate The event start {@link Date}.
     */
    public void setEventStartDate(Date eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    /**
     * Gets the categories associated with the event.
     *
     * @return A {@link List} of category strings.
     */
    public List<String> getCategories() {
        return categories != null ? new ArrayList<>(categories) : new ArrayList<>();
    }



    /**
     * Sets the categories for the event.
     *
     * @param categories A {@link List} of category strings.
     */
    public void setCategories(List<String> categories) {
        this.categories = new TreeSet<>(categories);
    }

    /**
     * Sets the tagline for the event.
     *
     * @param tagline The event tagline.
     */
    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    /**
     * Sets the title of the event.
     *
     * @param eventTitle The event title.
     */
    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    /**
     * Gets the local drawable resource ID for the event's image.
     * This method is annotated with {@link Exclude} to prevent the {@code image}
     * field from being serialized and stored in Firestore, as it is a local resource identifier.
     * @return The integer ID of the drawable resource.
     */
    @com.google.firebase.firestore.Exclude
    public int getImage() { return image; }

    /**
     * Gets the detailed description of the event.
     *
     * @return The event description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the detailed description of the event.
     * @param description The event description text.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the banner image stored as a Firestore Blob.
     * @return The banner image Blob, or null if not set.
     */
    public Blob getBannerImageBlob() {
        return bannerImageBlob;
    }

    /**
     * Sets the banner image as a Firestore Blob.
     * @param bannerImageBlob The banner image Blob to store.
     */
    public void setBannerImageBlob(Blob bannerImageBlob) {
        this.bannerImageBlob = bannerImageBlob;
    }

    /**
     * Gets the banner image as a Bitmap.
     * If the bitmap hasn't been decoded yet, it decodes it from the stored Blob.
     * This method is excluded from Firestore serialization.
     * @return The banner image Bitmap, or null if no image is set.
     */
    @com.google.firebase.firestore.Exclude
    public Bitmap getBannerImageBitmap() {
        if (bannerImage == null && bannerImageBlob != null) {
            byte[] bytes = bannerImageBlob.toBytes();
            bannerImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return bannerImage;
    }

    /**
     * Sets the banner image from a Bitmap.
     * The bitmap is automatically compressed to JPEG format and stored as a Blob.
     * @param bannerImage The banner image Bitmap to set.
     */
    public void setBannerImage(Bitmap bannerImage) {
        this.bannerImage = bannerImage;
        if (bannerImage != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bannerImage.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            this.bannerImageBlob = Blob.fromBytes(stream.toByteArray());
        }
    }

    // --- Location Support ---

    /**
     * Gets the name of the event location.
     * @return The location name string, or null if not set.
     */
    public String getEventLocationName() {
        return eventLocationName;
    }

    /**
     * Sets the name of the event location.
     * @param eventLocationName The location name to set.
     */
    public void setEventLocationName(String eventLocationName) {
        this.eventLocationName = eventLocationName;
    }

    /**
     * Gets the latitude coordinate of the event location.
     * @return The latitude as a Double, or null if not set.
     */
    public Double getLocationLatitude() {
        return locationLatitude;
    }

    /**
     * Sets the latitude coordinate of the event location.
     * @param locationLatitude The latitude to set.
     */
    public void setLocationLatitude(Double locationLatitude) {
        this.locationLatitude = locationLatitude;
    }

    /**
     * Gets the longitude coordinate of the event location.
     * @return The longitude as a Double, or null if not set.
     */
    public Double getLocationLongitude() {
        return locationLongitude;
    }

    /**
     * Sets the longitude coordinate of the event location.
     * @param locationLongitude The longitude to set.
     */
    public void setLocationLongitude(Double locationLongitude) {
        this.locationLongitude = locationLongitude;
    }

    /**
     * Helper to get location as GeoPoint for Firestore integration if needed.
     * Note: Firestore might map Double fields automatically, but if we want a specific GeoPoint field in DB,
     * we might need to construct it. However, keeping it simple with lat/long is safer for Serializable.
     * If we want Firestore to see a GeoPoint, we can add a getter marked with @PropertyName or just let it store fields.
     * Actually, Firestore handles GeoPoint specifically.
     * Let's add a pseudo-property that Firestore can use if we map it manually, or just use lat/long.
     * To keep it simple and Serializable, I'll just use lat/long fields in the class.
     * But wait, if we want to query by location later, GeoPoint is better.
     * I will add a getter/setter for GeoPoint that updates the lat/long fields,
     * and mark the lat/long fields as @Exclude if I want only GeoPoint in DB, OR
     * mark GeoPoint as @Exclude if I want lat/long in DB.
     * The prompt says "saved into firebase". GeoPoint is standard.
     */

    /**
     * Gets the event location as a GeoPoint object.
     * This method is excluded from Firestore serialization.
     * @return A GeoPoint containing the latitude and longitude, or null if location is not set.
     */
    @Exclude
    public GeoPoint getEventGeoLocation() {
        if (locationLatitude != null && locationLongitude != null) {
            return new GeoPoint(locationLatitude, locationLongitude);
        }
        return null;
    }

    /**
     * Sets the event location from a GeoPoint object.
     * This method is excluded from Firestore serialization.
     * @param geoPoint The GeoPoint to set, or null to clear the location.
     */
    @Exclude
    public void setEventGeoLocation(GeoPoint geoPoint) {
        if (geoPoint != null) {
            this.locationLatitude = geoPoint.getLatitude();
            this.locationLongitude = geoPoint.getLongitude();
        } else {
            this.locationLatitude = null;
            this.locationLongitude = null;
        }
    }

    /**
     * Accessor for Firestore to save GeoPoint.
     * This is a bit tricky because if we have getGeoLocation and setGeoLocation, Firestore might use it.
     * But we also have lat/long fields.
     * Let's just store lat/long as doubles for now to avoid Serializable issues and Firestore complexities.
     * Google Maps can take lat/long easily.
     */


    /**
     * Builder class for constructing Event objects using the Builder pattern.
     * Provides a fluent interface for setting event properties.
     */
    public static class Builder {
        @UmlAssociate(selfCard = "1", label = "fills", otherCard = "1")
        private com.rocket.radar.eventmanagement.EventGeneralFragment eventGeneralFragment;

        @UmlAssociate(selfCard = "1", label = "fills", otherCard = "1")
        private com.rocket.radar.eventmanagement.EventLotteryFragment eventLotteryFragment;

        private Event event;

        public Builder() {
            event = new Event();
            event.eventId = UUID.randomUUID().toString();
        }
        
        public Builder(Event event) {
            this.event = event;
        }

        /**
         * Sets the event title.
         * @param title The event title.
         * @return This Builder instance for method chaining.
         */
        public Builder title(String title) {
            event.eventTitle = title;
            return this;
        }

        /**
         * Sets the event description.
         * @param description The detailed event description.
         * @return This Builder instance for method chaining.
         */
        public Builder description(String description) {
            event.description = description;
            return this;
        }

        /**
         * Sets the event tagline.
         * @param tagline A short, catchy phrase for the event.
         * @return This Builder instance for method chaining.
         */
        public Builder tagline(String tagline) {
            event.tagline = tagline;
            return this;
        }

        /**
         * Sets the event categories.
         * @param categories A list of category strings.
         * @return This Builder instance for method chaining.
         */
        public Builder categories(List<String> categories) {
            event.setCategories(categories);
            return this;
        }

        /**
         * Sets the banner image from a Bitmap.
         * @param bitmap The banner image as a Bitmap.
         * @return This Builder instance for method chaining.
         */
        public Builder bannerImage(Bitmap bitmap) {
            event.setBannerImage(bitmap);
            return this;
        }

        /**
         * Sets the waitlist capacity.
         * @param capacity The waitlist capacity wrapped in Optional, or empty Optional for unlimited.
         * @return This Builder instance for method chaining.
         */
        public Builder waitlistCapacity(Optional<Integer> capacity) {
            event.waitlistCapacity = capacity;
            return this;
        }

        /**
         * Sets the event capacity.
         * @param capacity The maximum number of attendees.
         * @return This Builder instance for method chaining.
         */
        public Builder eventCapacity(int capacity) {
            event.eventCapacity = capacity;
            return this;
        }

        /**
         * Sets the event start date.
         * @param date The date when the event starts.
         * @return This Builder instance for method chaining.
         */
        public Builder eventStartDate(Date date) {
            event.eventStartDate = date;
            return this;
        }

        /**
         * Sets the event end date.
         * @param date The date when the event ends.
         * @return This Builder instance for method chaining.
         */
        public Builder eventEndDate(Date date) {
            event.setEventEndDate(date);
            return this;
        }

        /**
         * Sets the event start time.
         * @param time The time when the event starts.
         * @return This Builder instance for method chaining.
         */
        public Builder eventStartTime(Time time) {
            event.eventStartTime = time;
            return this;
        }

        /**
         * Sets the event end time.
         * @param time The time when the event ends.
         * @return This Builder instance for method chaining.
         */
        public Builder eventEndTime(Time time) {
            event.eventEndTime = time;
            return this;
        }

        /**
         * Sets the registration start date.
         * @param date The date when registration opens.
         * @return This Builder instance for method chaining.
         */
        public Builder registrationStartDate(Date date) {
            event.registrationStartDate = date;
            return this;
        }

        /**
         * Sets the initial selection start date.
         * @param date The date when the initial lottery selection begins.
         * @return This Builder instance for method chaining.
         */
        public Builder initialSelectionStartDate(Date date) {
            event.selectionStartDate = date;
            return this;
        }

        /**
         * Sets the final selection date.
         * @param date The final date for lottery selection.
         * @return This Builder instance for method chaining.
         */
        public Builder finalSelectionDate(Date date) {
            event.finalSelectionDate = date;
            return this;
        }

        /**
         * Sets the event location.
         * @param geoPoint The geographic coordinates of the event location.
         * @param name The name of the event location.
         * @return This Builder instance for method chaining.
         */
        public Builder location(GeoPoint geoPoint, String name) {
            event.setEventGeoLocation(geoPoint);
            event.setEventLocationName(name);
            return this;
        }

        /**
         * Sets whether location tracking is required for this event.
         * @param require true if location is required, false otherwise.
         * @return This Builder instance for method chaining.
         */
        public Builder requireLocation(boolean require) {
            event.setRequireLocation(require);
            return this;
        }

        /**
         * Builds and returns the Event object.
         * @return The constructed Event instance.
         */
        public Event build() {
            return event;
        }
    }

    /**
     * Gets the list of user IDs on the event waitlist.
     * @return An ArrayList of user IDs, or an empty list if none are waitlisted.
     */
    public ArrayList<String> getEventWaitlistIds() {
        return eventWaitlistIds != null ? eventWaitlistIds : new ArrayList<>();
    }

    /**
     * Gets the list of user IDs who have been invited to the event.
     * @return An ArrayList of user IDs, or an empty list if none are invited.
     */
    public ArrayList<String> getEventInvitedIds() {
        return eventInvitedIds != null ? eventInvitedIds : new ArrayList<>();
    }

    /**
     * Gets the list of user IDs who are attending the event.
     * @return An ArrayList of user IDs, or an empty list if none are attending.
     */
    public ArrayList<String> getEventAttendingIds() {
        return eventAttendingIds != null ? eventAttendingIds : new ArrayList<>();
    }

    /**
     * Gets the list of user IDs who have cancelled their event registration.
     * @return An ArrayList of user IDs, or an empty list if none have cancelled.
     */
    public ArrayList<String> getEventCancelledIds() {
        return eventCancelledIds != null ? eventCancelledIds : new ArrayList<>();
    }
}
