package com.rocket.radar.events;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * ViewModel for managing event filters in the application.
 * Maintains a collection of EventFilter instances and applies them to event streams.
 */
public class FilterModel extends ViewModel {
    /**
     * Abstract base class for event filters.
     * Subclasses must implement the filter logic and provide a filter name.
     */
    public abstract static class EventFilter {
        /**
         * Tests whether an event passes this filter.
         * @param event the event to test
         * @return true if the event passes the filter, false otherwise
         */
        public abstract boolean filter(Event event);

        /**
         * Gets the display name of this filter.
         * @return the filter name
         */
        @NonNull public abstract String getFilterName();

        /**
         * Gets the category this filter belongs to.
         * @return the filter category, or null if not categorized
         */
        protected String getFilterCategory() { return null; }
    }

    private ArrayList<EventFilter> filters = new ArrayList<>();

    /**
     * Gets a stream of all non-null active filters.
     * @return a stream of active EventFilter instances
     */
    public Stream<EventFilter> getFilters() {
        return filters.stream().filter(Objects::nonNull);
    }

    /**
     * Adds a filter to the model. If there are null positions (from removed filters),
     * the filter will fill the first available position; otherwise, it is added to the end.
     * @param eventFilter the filter to add
     * @return the ID (index) of the added filter
     */
    public int addFilter(EventFilter eventFilter) {
        // See if there are any null positions we can fill this filter into.
        for (int i = 0; i < filters.size(); ++i) {
            if (filters.get(i) == null) {
                filters.set(i, eventFilter);
                return i;
            }
        }

        // No free positions add another filter to list
        int filterId = filters.size();
        filters.add(eventFilter);
        return filterId;
    }

    /**
     * Removes all filters with the specified name by setting their positions to null.
     * @param filterName the name of the filter(s) to remove
     */
    public void removeFilterByName(@NonNull String filterName) {
        for (int i = 0; i < filters.size(); ++i) {
            EventFilter eventFilter = filters.get(i);
            if (eventFilter != null && eventFilter.getFilterName().equals(filterName)) {
                filters.set(i, null);
            }
        }
    }

    /**
     * Removes a filter at the specified ID by setting its position to null.
     * @param filterId the ID (index) of the filter to remove
     */
    public void removeFilterById(int filterId) {
        filters.set(filterId, null);
    }

    /**
     * Removes all filters belonging to the specified category by setting their positions to null.
     * @param category the category of filters to remove
     */
    public void removeFiltersByCategory(String category) {
        for (int i = 0; i < filters.size(); ++i) {
            EventFilter eventFilter = filters.get(i);
            if (eventFilter != null && eventFilter.getFilterCategory().equals(category)) {
                filters.set(i, null);
            }
        }
    }

    /**
     * Removes all filters from the model by clearing the filter list.
     */
    public void clearFilters() {
        filters.clear();
    }

    /**
     * Applies all active filters to a stream of events.
     * Each non-null filter is applied sequentially to the event stream.
     * @param events the stream of events to filter
     * @return the filtered stream of events
     */
    public Stream<Event> filter(Stream<Event> events) {
        for (var eventFilter : filters) {
            if (eventFilter == null) continue;
            events = events.filter(eventFilter::filter);
        }
        return events;
    }
}
