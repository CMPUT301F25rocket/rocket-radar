package com.rocket.radar.eventmanagement;

import com.rocket.radar.events.Event;

/**
 * Interface for fragments that contribute input to the event creation/editing process.
 * Implementing fragments are responsible for validating their inputs and extracting
 * their data into an Event.Builder object.
 */
public interface InputFragment {
    /**
     * Validates the input provided by this fragment.
     * @param inputFragment The fragment to validate.
     * @return true if the input is valid, false otherwise.
     */
    boolean valid(InputFragment inputFragment);

    /**
     * Extracts the fragment's input data and applies it to the provided Event.Builder.
     * @param builder The Event.Builder to populate with data from this fragment.
     * @return The modified Event.Builder with this fragment's data applied.
     * @throws Exception if an error occurs during data extraction.
     */
    Event.Builder extract(Event.Builder builder) throws Exception;
}
