package com.rocket.radar.eventmanagement;

import com.rocket.radar.events.Event;

public interface InputFragment {
    boolean valid(InputFragment inputFragment);

    Event.Builder extract(Event.Builder builder);
}
