package com.rocket.radar.events;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
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
 * This class serves as a data model for event information, including its title, date,
 * and a unique identifier. It is designed to be serialized for passing between
 * Android components (e.g., Fragments) and is compatible with Google Firestore for
 * database operations. The {@link Serializable} interface allows Event objects to be
 * passed in Bundles.
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

    // Join Management
    ArrayList<String> eventWaitlistIds;
    ArrayList<String> eventInvitedIds;
    ArrayList<String> eventAttendingIds;
    ArrayList<String> eventCancelledIds;

    int image;

    public Event() {
        // Default constructor required
        bannerImage = null;
    }

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


    public int getColor() {
        return color.toArgb();
    }

    public void setColor(int color) {
        this.color = Color.valueOf(color);
    }

    public Time getLotteryTime() {
        return lotteryTime;
    }

    public void setLotteryTime(Time lotteryTime) {
        this.lotteryTime = lotteryTime;
    }

    public Date getLotteryDate() {
        return lotteryDate;
    }

    public void setLotteryDate(Date lotteryDate) {
        this.lotteryDate = lotteryDate;
    }

    public int getEventCapacity() {
        return eventCapacity;
    }

    public void setEventCapacity(int eventCapacity) {
        this.eventCapacity = eventCapacity;
    }

    public boolean isRequireLocation() {
        return requireLocation;
    }

    public void setRequireLocation(boolean requireLocation) {
        this.requireLocation = requireLocation;
    }

    public Integer getWaitlistCapacity() {
        return waitlistCapacity.orElse(null);
    }

    public void setWaitlistCapacity(Integer waitlistCapacity) {
        this.waitlistCapacity = Optional.ofNullable(waitlistCapacity);
    }

    public Date getFinalSelectionDate() {
        return finalSelectionDate;
    }

    public void setFinalSelectionDate(Date finalSelectionDate) {
        this.finalSelectionDate = finalSelectionDate;
    }

    public Date getSelectionEndDate() {
        return selectionEndDate;
    }

    public void setSelectionEndDate(Date selectionEndDate) {
        this.selectionEndDate = selectionEndDate;
    }

    public Date getSelectionStartDate() {
        return selectionStartDate;
    }

    public void setSelectionStartDate(Date selectionStartDate) {
        this.selectionStartDate = selectionStartDate;
    }

    public Date getRegistrationEndDate() {
        return registrationEndDate;
    }

    public void setRegistrationEndDate(Date registrationEndDate) {
        this.registrationEndDate = registrationEndDate;
    }

    public Date getRegistrationStartDate() {
        return registrationStartDate;
    }

    public void setRegistrationStartDate(Date registrationStartDate) {
        this.registrationStartDate = registrationStartDate;
    }

    public Time getEventEndTime() {
        return eventEndTime;
    }

    public void setEventEndTime(Time eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    public Time getEventStartTime() {
        return eventStartTime;
    }

    public void setEventStartTime(Time eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    public Date getEventEndDate() {
        return eventEndDate.orElse(null);
    }

    public void setEventEndDate(Date eventEndDate) {
        this.eventEndDate = Optional.ofNullable(eventEndDate);
    }

    public void setEventStartDate(Date eventStartDate) {
        this.eventStartDate = eventStartDate;
    }

    public List<String> getCategories() {
        return new ArrayList<>(categories);
    }

    public void setCategories(List<String> categories) {
        this.categories = new TreeSet<>(categories);
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

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

    public String getDescription() {
        return description;
    }

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

    public Blob getBannerImageBlob() { return this.bannerImageBlob; }
    public void setBannerImageBlob(Blob data) { this.bannerImageBlob = data; }

    private static Bitmap resizeBanner(Bitmap image)
    throws Exception {
        // IMPORTANT: Make sure these are kept up to date.
        // These are the dimensions in DP for the image on the event view page. This may result
        // in blurriness on HDPI screens (not really sure) but we're covering it with a gradient
        // anyways so we should be good.
        final int targetWidth = 420;
        final int targetHeight = 350;

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

    public ArrayList<String> getEventCancelledIds() {
        return eventCancelledIds;
    }

    public void setEventCancelledIds(ArrayList<String> eventCancelledIds) {
        this.eventCancelledIds = eventCancelledIds;
    }

    public ArrayList<String> getEventAttendingIds() {
        if (eventAttendingIds == null) {
            eventAttendingIds = new ArrayList<>();
        }
        return eventAttendingIds;
    }

    public void setEventAttendingIds(ArrayList<String> eventAttendingIds) {
        this.eventAttendingIds = eventAttendingIds;
    }

    public ArrayList<String> getEventInvitedIds() {
        if (eventInvitedIds == null) {
            eventInvitedIds = new ArrayList<>();
        }
        return eventInvitedIds;
    }

    public void setEventInvitedIds(ArrayList<String> eventInvitedIds) {
        this.eventInvitedIds = eventInvitedIds;
    }

    public ArrayList<String> getEventWaitlistIds() {
        if (eventWaitlistIds == null) {
            eventWaitlistIds = new ArrayList<>();
        }
        return eventWaitlistIds;
    }

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
        public Builder categories(ArrayList<String> categories) {
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
            Bitmap resized = resizeBanner(image);
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
