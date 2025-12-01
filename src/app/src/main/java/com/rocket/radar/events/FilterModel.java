package com.rocket.radar.events;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Stream;

public class FilterModel extends ViewModel {
    public abstract static class EventFilter {
        public abstract boolean filter(Event event);
        @NonNull public abstract String getFilterName();
        protected String getFilterCategory() { return null; }
    }

    private ArrayList<EventFilter> filters = new ArrayList<>();

    public Stream<EventFilter> getFilters() {
        return filters.stream().filter(Objects::nonNull);
    }

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

    public void removeFilterByName(@NonNull String filterName) {
        for (int i = 0; i < filters.size(); ++i) {
            EventFilter eventFilter = filters.get(i);
            if (eventFilter != null && eventFilter.getFilterName().equals(filterName)) {
                filters.set(i, null);
            }
        }
    }

    public void removeFilterById(int filterId) {
        filters.set(filterId, null);
    }

    public void removeFiltersByCategory(String category) {
        for (int i = 0; i < filters.size(); ++i) {
            EventFilter eventFilter = filters.get(i);
            if (eventFilter != null && eventFilter.getFilterCategory().equals(category)) {
                filters.set(i, null);
            }
        }
    }

    public void clearFilters() {
        filters.clear();
    }

    public Stream<Event> filter(Stream<Event> events) {
        for (var eventFilter : filters) {
            if (eventFilter == null) continue;
            events = events.filter(eventFilter::filter);
        }
        return events;
    }
}
