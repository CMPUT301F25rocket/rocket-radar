package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rocket.radar.eventmanagement.Time;
import com.rocket.radar.events.Event;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Shared test utilities for Event testing across unit and instrumented tests.
 */
public class EventTestUtils {

    /**
     * Draws a black square in the middle of a canvas 1/5th the size of the smallest dimension.
     * @param canvas Some filled canvas.
     */
    public static void drawSampleImage(Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        int halfDelta = Integer.min(canvas.getWidth(), canvas.getHeight()) / 10;
        int centerX = canvas.getWidth() / 2;
        int centerY = canvas.getHeight() / 2;

        Rect region = new Rect(centerX - halfDelta, centerY-halfDelta, centerX + halfDelta, centerY + halfDelta);
        canvas.drawRect(region, paint);
    }

    /**
     * Creates a sample event for testing purposes.
     */
    public static Event sampleEvent() throws Exception {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        Event event = new Event();
        event.setEventTitle("BBQ Event");

        cal.set(currentYear, Calendar.OCTOBER, 20);
        event.setEventStartDate(cal.getTime());

        cal.set(currentYear, Calendar.OCTOBER, 24);
        event.setEventEndDate(cal.getTime());

        event.setTagline("Mushroom bros who listen to bangers");

        ArrayList<String> cats = new ArrayList<>();
        cats.add(Event.allEventCategories.get(2));
        cats.add(Event.allEventCategories.get(4));
        event.setCategories(cats);

        event.setDescription("A chill BBQ event for everyone who enjoys good music and even better food. We'll be grilling up a storm and spinning some bangers. Come hang out!");

        event.setEventStartTime(new Time(2, 30));
        event.setEventEndTime(new Time(4, 40));

        cal.set(currentYear, Calendar.SEPTEMBER, 2);
        event.setRegistrationStartDate(cal.getTime());
        cal.set(currentYear, Calendar.SEPTEMBER, 5);
        event.setRegistrationEndDate(cal.getTime());

        cal.set(currentYear, Calendar.SEPTEMBER, 6);
        event.setSelectionStartDate(cal.getTime());
        cal.set(currentYear, Calendar.SEPTEMBER, 8);
        event.setSelectionEndDate(cal.getTime());
        cal.set(currentYear, Calendar.SEPTEMBER, 14);
        event.setFinalSelectionDate(cal.getTime());

        event.setWaitlistCapacity(30);
        event.setRequireLocation(true);
        cal.set(currentYear, Calendar.SEPTEMBER, 10);
        event.setLotteryDate(cal.getTime());
        event.setLotteryTime(new Time(12, 0));
        event.setColor(Color.RED);

        Bitmap bitmap = Bitmap.createBitmap(500, 880, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        drawSampleImage(canvas);

        return new Event.Builder(event)
                .bannerImage(bitmap)
                .build();
    }

    /**
     * Asserts that two events are equal based on key fields.
     */
    public static void assertEventEquals(@NonNull Event expected, @Nullable Event actual) {
        assertNotNull("Actual event is null", actual);
        assertEquals("Mismatch title", expected.getEventTitle(), actual.getEventTitle());
        assertEquals("Mismatch event start date", expected.getEventStartDate(), actual.getEventStartDate());
        assertEquals("Mismatch event end date", expected.getEventEndDate(), actual.getEventEndDate());
    }
}
