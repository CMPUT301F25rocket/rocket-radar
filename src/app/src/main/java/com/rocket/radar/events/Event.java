package com.rocket.radar.events;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Date;
import java.time.format.TextStyle;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import java.util.Optional;
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
    private String eventId; // ADD THIS FIELD
    String eventTitle;
    private Date date;
    String tagline;
    String description;

    private Blob bannerImageBlob;

    // WARN: DO NOT REMOVE TRANSIENT. WE WILL CONSUME OUR FIRESTORE USAGE FAST (maybe).
    // We don't want this one serialized.
    private transient Bitmap bannerImage;


    int image;

    public Event() {
        // Default constructor required
        bannerImage = null;
    }

    public Event(String eventTitle, Date date, String tagline, String description, int image) {
        this.eventId = UUID.randomUUID().toString(); // Generate a unique ID
        this.eventTitle = eventTitle;
        this.date = date; // Assuming date is in "YYYY-MM-DD" format
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
    public Date getDate() { return date != null ? date: null;}
    public String getFormattedDate() {
        // returns the date in format DD\nMMM where MMM three letter capital abbreviation for the month
        if (date == null) return "";
        LocalDate localDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
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

    public static class Builder {
        private Event event;

        public Builder() {
            event = new Event();
            event.eventId = UUID.randomUUID().toString(); // Generate a unique ID
        }

        public Builder title(String title) {
            event.eventTitle = title;
            return this;
        }

        public Builder eventStartDate(Date date) {
            // TODO
            return this;
        }

        public Builder eventEndDate() {
            // TODO
            return this;
        }

        public Builder tagline(String tagline) {
            event.tagline = tagline;
            return this;
        }


        public Builder description(String description) {
            new Event();
            event.description = description;
            return this;
        }

        public Builder eventStartTime(Time time) {
            // TODO
            return this;
        }

        public Builder eventEndTime(Time time) {
            // TODO
            return this;
        }

        public Builder registrationStartDate(Date date) {
            // TODO
            return this;
        }

        public Builder registrationEndDate(Date date) {
            // TODO
            return this;
        }

        public Builder initialSelectionStartDate(Date date) {
            // TODO
            return this;
        }

        public Builder initialSelectionEndDate(Date date) {
            // TODO
            return this;
        }

        public Builder finalSelectionDate(Date date) {
            // TODO
            return this;
        }

        public Builder waitlistCapacity(Optional<Integer> capacity) {
            // TODO
            return this;
        }

        public Builder requireLocation(Boolean value) {
            // TODO
            return this;
        }

        public Builder eventCapacity(Integer capacity) {
            // TODO
            return this;
        }

        public Builder lotteryDate(Date date) {
            // TODO
            return this;
        }

        public Builder lotteryTime(Time time) {
            // TODO
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
            // TODO
            return this;
        }

        public Event build() {
            return event;
        }
    }
}
