package com.rocket.radar;

import com.rocket.radar.events.Event;

import java.util.Calendar;
import java.util.List;

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
}
