package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.google.firebase.firestore.GeoPoint;
import com.rocket.radar.events.Event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class EventsTests {

    /**
     * Draws a black square in the middle of a canvas 1/5th the size of the smallest dimension.
     * @param canvas Some filled canvas.
     */
    public void drawSampleImage(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        int halfDelta = Integer.min(canvas.getWidth(), canvas.getHeight()) / 10;
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;

        Rect region = new Rect(centerX - halfDelta, centerY-halfDelta, centerX + halfDelta, centerY + halfDelta);
        canvas.drawRect(region, paint);
    }

    /**
     * Compute the absolute error between two images. Precondition: the bitmaps have the same size and
     * are in the same color space.
     * @param measuredBitmap
     * @param trueBitmap
     * @return
     */
    public float bitmapAbsoluteError(Bitmap measuredBitmap, Bitmap trueBitmap) {
        float error = 0;
        for (int row = 0; row < trueBitmap.getHeight(); ++row) {
            for (int col = 0; col < trueBitmap.getWidth(); ++col)  {
                Color measuredPixel = Color.valueOf(measuredBitmap.getPixel(col, row));
                Color truePixel = Color.valueOf(trueBitmap.getPixel(col, row));
                float[] measuredComponents = measuredPixel.getComponents();
                float[] trueComponents = truePixel.getComponents();
                for (int i = 0; i < trueComponents.length; ++i) {
                    error += measuredComponents[i] - trueComponents[i];
                }
            }
        }
        return error;
    }

    @Test
    public void testImageSizeReduction() {
        int width = 420;
        int height = 350;
        Bitmap.Config config = Bitmap.Config.ARGB_8888;

        Bitmap bigBitmap = Bitmap.createBitmap(width * 2, height * 2, config);
        Canvas bigCanvas = new Canvas(bigBitmap);
        bigCanvas.drawColor(Color.WHITE);
        drawSampleImage(bigCanvas);

        Bitmap actual = Event.resizeBanner(bigBitmap, width, height);
        assertEquals("Resize gave the wrong width", width, actual.getWidth());
        assertEquals("Resize gave the wrong height", height, actual.getHeight());

        Bitmap expected = Bitmap.createBitmap(width, height, config);
        Canvas littleCanvas = new Canvas(expected);
        littleCanvas.drawColor(Color.WHITE);
        drawSampleImage(littleCanvas);

        float error = bitmapAbsoluteError(actual, expected);
        assertTrue("Error is not within tolerance: " + error, error < 1e-7);
    }

    @Test
    public void testEventBuilder() throws Exception {
        Event sample = EventTestUtils.sampleEvent();


        // Make a *mutable copy* so we don't accidentally mutate the original list
        List<String> categories = new ArrayList<>(sample.getCategories());

        Event copy = new Event.Builder()
                .title(sample.getEventTitle())
                .eventStartDate(sample.getEventStartDate())
                .eventEndDate(sample.getEventEndDate())
                .tagline(sample.getTagline())
                .categories(categories)
                .description(sample.getDescription())
                .eventStartTime(sample.getEventStartTime())
                .eventEndTime(sample.getEventEndTime())
                .registrationStartDate(sample.getRegistrationStartDate())
                .initialSelectionStartDate(sample.getSelectionStartDate())
                .finalSelectionDate(sample.getFinalSelectionDate())
                .waitlistCapacity(Optional.ofNullable(sample.getWaitlistCapacity()))
                .requireLocation(sample.isRequireLocation())
                .bannerImage(sample.getBannerImageBitmap())
                .build();

        EventTestUtils.assertEventEquals(sample, copy);
    }

    @Test
    public void testGetFormattedDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.OCTOBER, 31); // Month is 0-indexed, October is 9
        Date date = cal.getTime();

        Event event = new Event();
        event.setEventStartDate(date);

        // Expected: 31\nOCT
        String formatted = event.getFormattedDate();
        assertTrue("Formatted date should contain day", formatted.contains("31"));
        assertTrue("Formatted date should contain short month", formatted.contains("OCT"));
    }

    @Test
    public void testGeoLocationMapping() {
        Event event = new Event();
        
        // Test Setting GeoPoint updates lat/long
        GeoPoint gp = new GeoPoint(53.5461, -113.4938);
        event.setEventGeoLocation(gp);
        
        assertEquals(53.5461, event.getLocationLatitude(), 0.0001);
        assertEquals(-113.4938, event.getLocationLongitude(), 0.0001);
        
        // Test Getting GeoPoint
        GeoPoint retrieved = event.getEventGeoLocation();
        assertNotNull(retrieved);
        assertEquals(gp.getLatitude(), retrieved.getLatitude(), 0.0001);
        assertEquals(gp.getLongitude(), retrieved.getLongitude(), 0.0001);
        
        // Test null
        event.setEventGeoLocation(null);
        assertNull(event.getLocationLatitude());
        assertNull(event.getLocationLongitude());
        assertNull(event.getEventGeoLocation());
    }

    @Test
    public void testListsInitialization() {
        Event event = new Event();
        
        // Lists should handle null internal state gracefully by returning empty lists
        assertNotNull(event.getCategories());
        assertTrue(event.getCategories().isEmpty());
        
        assertNotNull(event.getEventWaitlistIds());
        assertTrue(event.getEventWaitlistIds().isEmpty());
        
        assertNotNull(event.getEventInvitedIds());
        assertTrue(event.getEventInvitedIds().isEmpty());
        
        assertNotNull(event.getEventAttendingIds());
        assertTrue(event.getEventAttendingIds().isEmpty());
        
        assertNotNull(event.getEventCancelledIds());
        assertTrue(event.getEventCancelledIds().isEmpty());
    }
    
    @Test
    public void testConstructor() {
        Date now = new Date();
        Event event = new Event("Title", now, "Tagline", "Description", 123);
        
        assertNotNull("Event ID should be generated", event.getEventId());
        assertEquals("Title", event.getEventTitle());
        assertEquals(now, event.getEventStartDate());
        assertEquals("Tagline", event.getTagline());
        assertEquals("Description", event.getDescription());
        assertEquals(123, event.getImage());
    }
}
