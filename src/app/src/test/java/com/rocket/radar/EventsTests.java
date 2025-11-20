package com.rocket.radar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rocket.radar.eventmanagement.Time;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

public class EventsTests {
    // This method just prepares the local list of dummy data.
    private List<Event> loadDummyData() {
        List<Event> eventList = new java.util.ArrayList<>();

        // Using Calendar to create Date objects for the current year
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        cal.set(currentYear, Calendar.SEPTEMBER, 30);
        eventList.add(new Event("Watch Party for Oilers", cal.getTime(), "Fun for fanatics", "Join us for an exciting watch party as the Oilers take on their rivals. Great food, great company, and a thrilling game await. Don't miss out on the action!", R.drawable.rogers_image));
        cal.set(currentYear, Calendar.NOVEMBER, 12);
        eventList.add(new Event("BBQ Event", cal.getTime(), "Mushroom bros who listen to bangers", "A chill BBQ event for everyone who enjoys good music and even better food. We'll be grilling up a storm and spinning some bangers. Come hang out!", R.drawable.mushroom_in_headphones_amidst_nature));
        cal.set(currentYear, Calendar.DECEMBER, 18);
        eventList.add(new Event("Ski Trip", cal.getTime(), "The slopes are calling", "Hit the slopes with us for a weekend of skiing and snowboarding. All skill levels are welcome. Get ready for some fresh powder and stunning mountain views.", R.drawable.ski_trip_banner));
        cal.set(currentYear + 1, Calendar.JANUARY, 5); // Next year for January
        eventList.add(new Event("Tech Conference", cal.getTime(), "Innovations in AI", "Discover the latest breakthroughs in Artificial Intelligence at our annual Tech Conference. Featuring keynote speakers from leading tech companies and interactive workshops.", R.drawable.rogers_image));
        cal.set(currentYear, Calendar.JULY, 22);
        eventList.add(new Event("Summer Music Festival", cal.getTime(), "Live bands and good vibes", "Experience the best of summer with our annual music festival. Featuring a lineup of incredible live bands, food trucks, and a vibrant atmosphere. Let the good times roll!", R.drawable.mushroom_in_headphones_amidst_nature));
        cal.set(currentYear, Calendar.AUGUST, 14);
        eventList.add(new Event("Mountain Hike", cal.getTime(), "Explore scenic trails", "Join our guided hike through breathtaking mountain trails. This is a great opportunity to connect with nature, get some exercise, and enjoy panoramic views.", R.drawable.ski_trip_banner));

        return eventList;
    }

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
        for (int row = 0; row < trueBitmap.getWidth(); ++row) {
            for (int col = 0; col < trueBitmap.getHeight(); ++col)  {
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
    public void testImageSizeReduction() throws Exception {
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

    public Event sampleEvent() throws Exception {
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
        
        Bitmap bitmap = Bitmap.createBitmap(500, 880, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        drawSampleImage(canvas);

        return new Event.Builder(event)
                .bannerImage(bitmap)
                .build();
    }

    public void assertEventEquals(@NonNull Event expected, @Nullable Event actual) {
        assertNotNull("Actual event is null", actual);
        assertEquals("Mismatch title", expected.getEventTitle(), actual.getEventTitle());
        assertEquals("Mismatch event start date", expected.getEventStartDate(), actual.getEventStartDate());
        assertEquals("Mismatch event end date", expected.getEventEndDate(), actual.getEventEndDate());
    }

    @Test
    public void testEventBuilder()
    throws Exception
    {
        Event sample = sampleEvent();
        List<String> categories = sample.getCategories();
        String first = categories.removeFirst();
        String last = categories.removeLast();

        Event copy = new Event.Builder()
                .title(sample.getEventTitle())
                .eventStartDate(sample.getEventStartDate())
                .eventEndDate(sample.getEventEndDate())
                .tagline(sample.getTagline())
                .category(last)
                .categories(categories)
                .category(first)
                .category(first)
                .description(sample.getDescription())
                .eventStartTime(sample.getEventStartTime())
                .eventEndTime(sample.getEventEndTime())
                .registrationStartDate(sample.getRegistrationStartDate())
                .registrationEndDate(sample.getRegistrationEndDate())
                .initialSelectionStartDate(sample.getSelectionStartDate())
                .initialSelectionEndDate(sample.getSelectionEndDate())
                .finalSelectionDate(sample.getFinalSelectionDate())
                .waitlistCapacity(Optional.ofNullable(sample.getWaitlistCapacity()))
                .requireLocation(sample.isRequireLocation())
                .lotteryDate(sample.getLotteryDate())
                .lotteryTime(sample.getLotteryTime())
                .bannerImage(sample.getBannerImageBitmap())
                .color(Color.valueOf(sample.getColor()))
                .build();
        assertEventEquals(sample, copy);
    }

    @Test
    public void testEventCreation()
    throws Exception
    {
        EventRepository repository = EventRepository.getInstance();
        Event sample = sampleEvent();
        repository.createEvent(sample);
        Event remoteEvent = FirebaseFirestore.getInstance()
                .collection("events")
                .document(sample.getEventId())
                .get()
                .getResult()
                .toObject(Event.class);
        assertEventEquals(sample, remoteEvent);
    }
}
