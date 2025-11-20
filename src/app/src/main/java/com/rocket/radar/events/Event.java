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
import com.rocket.radar.eventmanagement.Time;

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
    private Date registrationEndDate;
    private Date selectionStartDate;
    private Date selectionEndDate;
    private Date finalSelectionDate;
    private Optional<Integer> waitlistCapacity;
    private boolean requireLocation;
    private int eventCapacity;
    private Date lotteryDate;

    private Time lotteryTime;

    private Blob bannerImageBlob;

    // WARN: DO NOT REMOVE TRANSIENT. WE WILL CONSUME OUR FIRESTORE USAGE FAST (maybe).
    // We don't want this one serialized.
    private transient Bitmap bannerImage;
    private Color color;

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
     * Gets the associated color for the event as an ARGB integer.
     *
     * @return The integer representation of the event's color.
     */
    public int getColor() {
        return color.toArgb();
    }

    /**
     * Sets the color for the event from an ARGB integer.
     *
     * @param color The integer representation of the color.
     */
    public void setColor(int color) {
        this.color = Color.valueOf(color);
    }

    /**
     * Gets the time of the lottery draw.
     *
     * @return The {@link Time} of the lottery.
     */
    public Time getLotteryTime() {
        return lotteryTime;
    }

    /**
     * Sets the time of the lottery draw.
     *
     * @param lotteryTime The {@link Time} of the lottery.
     */
    public void setLotteryTime(Time lotteryTime) {
        this.lotteryTime = lotteryTime;
    }

    /**
     * Gets the date of the lottery draw.
     *
     * @return The {@link Date} of the lottery.
     */
    public Date getLotteryDate() {
        return lotteryDate;
    }

    /**
     * Sets the date of the lottery draw.
     *
     * @param lotteryDate The {@link Date} of the lottery.
     */
    public void setLotteryDate(Date lotteryDate) {
        this.lotteryDate = lotteryDate;
    }

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
        return waitlistCapacity.orElse(null);
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
     * Gets the end date of the selection period.
     *
     * @return The selection end {@link Date}.
     */
    public Date getSelectionEndDate() {
        return selectionEndDate;
    }

    /**
     * Sets the end date of the selection period.
     *
     * @param selectionEndDate The selection end {@link Date}.
     */
    public void setSelectionEndDate(Date selectionEndDate) {
        this.selectionEndDate = selectionEndDate;
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
     * Gets the end date of the registration period.
     *
     * @return The registration end {@link Date}.
     */
    public Date getRegistrationEndDate() {
        return registrationEndDate;
    }

    /**
     * Sets the end date of the registration period.
     *
     * @param registrationEndDate The registration end {@link Date}.
     */
    public void setRegistrationEndDate(Date registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
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
        return eventEndDate.orElse(null);
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
        return new ArrayList<>(categories);
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
     *
     * @param description The event description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Fetches the banner image. If it has not yet been decoded it will decode the image from the
     * base64 image field.
     * @return the banner image.
     */
    @com.google.firebase.firestore.Exclude
    public Bitmap getBannerImageBitmap() {
        if (bannerImage == null) {
            byte[] compressedBlob = bannerImageBlob.toBytes();
            bannerImage = BitmapFactory.decodeByteArray(compressedBlob, 0, compressedBlob.length);
        }
        return bannerImage;
    }

    /**
     * Gets the banner image as a Firestore {@link Blob}.
     * This is the format used for database storage.
     *
     * @return The banner image as a Blob.
     */
    public Blob getBannerImageBlob() { return this.bannerImageBlob; }
    /**
     * Sets the banner image from a Firestore {@link Blob}.
     *
     * @param data The banner image data as a Blob.
     */
    public void setBannerImageBlob(Blob data) { this.bannerImageBlob = data; }

    public static Bitmap resizeBanner(Bitmap image, final int targetWidth, final int targetHeight)
    throws Exception {
        int w = image.getWidth();
        int h = image.getHeight();
        if (w < targetWidth || h < targetHeight)
            throw new IllegalArgumentException("Expected " + targetWidth + "x" + targetHeight + " image or larger");

        Bitmap oneDimensionFit;
        if (w > targetWidth || h > targetHeight) {
            // Rescale the image to fit the smaller dimension. This will get rid of more pixels
            // than the crop so we do this first to reduce memory usage. In the case of a square
            // we crop excess height.
            if (w <= h) {
                int scaleHeight = Math.round(h * ((float)targetWidth / w));
                oneDimensionFit = Bitmap.createScaledBitmap(image, targetWidth,  scaleHeight, true);
            } else {
                int scaleWidth = Math.round(w * ((float)targetHeight / h));
                oneDimensionFit = Bitmap.createScaledBitmap(image, scaleWidth, targetHeight, true);
            }
        } else {
            // Proof of early return correctness.
            // ~(w < targetWidth || h < targetHeight) && ~(w > targetWidth || h > targetHeight)
            // w >= targetWidth && h >= targetHeight) && w <= targetWidth && h <= targetHeight
            // w >= targetWidth && w <= targetWidth && h >= targetHeight)  && h <= targetHeight
            // w == targetWidth && h == targetHeight
            // If our dimensions already match our target go ahead and assign.
            return image;
        }

        // Ok one of the dimensions by this point fits the image size. Figure out which one, and
        // then along the other dimension center the image with a window of target size and crop
        // to fit.
        Bitmap cropped;
        if (oneDimensionFit.getWidth() == targetWidth) {
            // Crop along vertical axis.
            int verticalSpace = oneDimensionFit.getHeight() - targetHeight;
            cropped = Bitmap.createBitmap(oneDimensionFit, 0, verticalSpace / 2, targetWidth, targetHeight);
        } else if (oneDimensionFit.getHeight() == targetHeight) {
            // Crop along horizontal axis.
            int horizontalSpace = oneDimensionFit.getWidth() - targetWidth;
            cropped = Bitmap.createBitmap(oneDimensionFit, horizontalSpace / 2, 0, targetWidth, targetHeight);
        } else {
            throw new Exception("This should be unreachable");
        }

        return cropped;
    }

    /**
     * Gets the list of user IDs who have cancelled their registration.
     *
     * @return An {@link ArrayList} of user ID strings. Returns an empty list if null.
     */
    public ArrayList<String> getEventCancelledIds() {
        if (eventCancelledIds == null) {
            eventCancelledIds = new ArrayList<>();
        }
        return eventCancelledIds;
    }

    /**
     * Sets the list of user IDs who have cancelled their registration.
     *
     * @param eventCancelledIds An {@link ArrayList} of user ID strings.
     */
    public void setEventCancelledIds(ArrayList<String> eventCancelledIds) {
        this.eventCancelledIds = eventCancelledIds;
    }

    /**
     * Gets the list of user IDs attending the event.
     *
     * @return An {@link ArrayList} of user ID strings. Returns an empty list if null.
     */
    public ArrayList<String> getEventAttendingIds() {
        if (eventAttendingIds == null) {
            eventAttendingIds = new ArrayList<>();
        }
        return eventAttendingIds;
    }

    /**
     * Sets the list of user IDs attending the event.
     *
     * @param eventAttendingIds An {@link ArrayList} of user ID strings.
     */
    public void setEventAttendingIds(ArrayList<String> eventAttendingIds) {
        this.eventAttendingIds = eventAttendingIds;
    }

    /**
     * Gets the list of user IDs invited to the event.
     *
     * @return An {@link ArrayList} of user ID strings. Returns an empty list if null.
     */
    public ArrayList<String> getEventInvitedIds() {
        if (eventInvitedIds == null) {
            eventInvitedIds = new ArrayList<>();
        }
        return eventInvitedIds;
    }

    /**
     * Sets the list of user IDs invited to the event.
     *
     * @param eventInvitedIds An {@link ArrayList} of user ID strings.
     */
    public void setEventInvitedIds(ArrayList<String> eventInvitedIds) {
        this.eventInvitedIds = eventInvitedIds;
    }

    /**
     * Gets the list of user IDs on the event's waitlist.
     *
     * @return An {@link ArrayList} of user ID strings. Returns an empty list if null.
     */
    public ArrayList<String> getEventWaitlistIds() {
        if (eventWaitlistIds == null) {
            eventWaitlistIds = new ArrayList<>();
        }
        return eventWaitlistIds;
    }

    /**
     * Sets the list of user IDs on the event's waitlist.
     *
     * @param eventWaitlistIds An {@link ArrayList} of user ID strings.
     */
    public void setEventWaitlistIds(ArrayList<String> eventWaitlistIds) {
        this.eventWaitlistIds = eventWaitlistIds;
    }

    public static class Builder {
        private Event event;

        /**
         * Create a {@code Event.Builder} to modify an existing event.
         * @param event The event to modify.
         */
        public Builder(Event event) {
            this.event = event;
        }

        /**
         * Create a new event using the builder.
         */
        public Builder() {
            event = new Event();
            event.eventId = UUID.randomUUID().toString(); // Generate a unique ID
            event.categories = new TreeSet<>();
            event.waitlistCapacity = Optional.empty();
            event.eventEndDate = Optional.empty();
        }

        public Builder title(String title) {
            event.eventTitle = title;
            return this;
        }

        public Builder eventStartDate(Date date) {
            event.eventStartDate = date;
            return this;
        }

        public Builder eventEndDate(Date date) {
            event.eventEndDate = Optional.of(date);
            return this;
        }

        public Builder tagline(String tagline) {
            event.tagline = tagline;
            return this;
        }

        /**
         * Adds the categories in the array to this event.
         * @param categories
         * @return
         */
        public Builder categories(Collection<String> categories) {
            event.categories.addAll(categories);
            return this;
        }

        /**
         * Add a single category to this event.
         * @param category
         * @return
         */
        public Builder category(String category) {
            event.categories.add(category);
            return this;
        }

        public Builder description(String description) {
            new Event();
            event.description = description;
            return this;
        }

        public Builder eventStartTime(Time time) {
            event.eventStartTime = time;
            return this;
        }

        public Builder eventEndTime(Time time) {
            event.eventEndTime = time;
            return this;
        }

        public Builder registrationStartDate(Date date) {
            event.registrationStartDate = date;
            return this;
        }

        public Builder registrationEndDate(Date date) {
            event.registrationEndDate = date;
            return this;
        }

        public Builder initialSelectionStartDate(Date date) {
            event.selectionStartDate = date;
            return this;
        }

        public Builder initialSelectionEndDate(Date date) {
            event.selectionEndDate = date;
            return this;
        }

        public Builder finalSelectionDate(Date date) {
            event.finalSelectionDate = date;
            return this;
        }

        public Builder waitlistCapacity(Optional<Integer> capacity) {
            event.waitlistCapacity = capacity;
            return this;
        }

        public Builder requireLocation(Boolean value) {
            event.requireLocation = value;
            return this;
        }

        public Builder eventCapacity(Integer capacity) {
            event.eventCapacity = capacity;
            return this;
        }

        public Builder lotteryDate(Date date) {
            event.lotteryDate = date;
            return this;
        }

        public Builder lotteryTime(Time time) {
            event.lotteryTime = time;
            return this;
        }

        public Builder bannerImage(Bitmap image) throws Exception {
            // IMPORTANT: Make sure these are kept up to date.
            // These are the dimensions in DP for the image on the event view page. This may result
            // in blurriness on HDPI screens (not really sure) but we're covering it with a gradient
            // anyways so we should be good.
            Bitmap resized = resizeBanner(image, 420, 350);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            event.bannerImageBlob = Blob.fromBytes(outputStream.toByteArray());
            event.bannerImage = resized;
            return this;
        }

        public Builder color(Color color) {
            event.color = color;
            return this;
        }

        public Event build() {
            return event;
        }
    }
}
