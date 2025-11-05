package com.rocket.radar.eventmanagement;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.FirebaseFirestore;
import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;

public class CreateEventModel extends ViewModel {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");

    // Creation wizard state.
    private MutableLiveData<Section> section;

    // General section field values
    public MutableLiveData<String> title;
    public MutableLiveData<String> description;


    // Datetime section field values
    public MutableLiveData<Boolean> singleDayEvent;
    public MutableLiveData<Optional<Date>> eventDate;

    /**
     * Helper for all the date mappings that need to be written. By the way.
     * @param source
     * @return
     */
    public LiveData<String> dateDisplay(MutableLiveData<Optional<Date>> source) {
        return Transformations.map(source, date -> date.map(dateFormatter::format).orElse(""));
    }

    public LiveData<String> eventDateDisplay() {
        return dateDisplay(eventDate);
    }

    public MutableLiveData<Optional<Time>> eventStartTime;
    public MutableLiveData<Optional<Time>> eventEndTime;

    // Deadline section field values
    public MutableLiveData<Optional<Date>> registrationStartDate;
    public LiveData<String> registrationStartDateDisplay() {
        return dateDisplay(registrationStartDate);
    }

    public MutableLiveData<Optional<Date>> registrationEndDate;
    public LiveData<String> registrationEndDateDisplay() {
        return dateDisplay(registrationEndDate);
    }

    public MutableLiveData<Optional<Date>> initialSelectionStartDate;
    public LiveData<String> initialSelectionStartDateDisplay() {
        return dateDisplay(initialSelectionStartDate);
    }

    public MutableLiveData<Optional<Date>> initialSelectionEndDate;
    public LiveData<String> initialSelectionEndDateDisplay() {
        return dateDisplay(initialSelectionEndDate);
    }

    public MutableLiveData<Optional<Date>> finalAttendeeSelectionDate;

    public LiveData<String> finalAttendeeSelectionDateDisplay() {
        return dateDisplay(finalAttendeeSelectionDate);
    }

    // Lottery section field values
    public MutableLiveData<Boolean> hasWaitlistCapacity;
    public MutableLiveData<Boolean> hasLocationRequirement;
    public MutableLiveData<Optional<Integer>> waitlistCapacity;
    public LiveData<String> waitlistCapacityDisplay() {
        return Transformations.map(waitlistCapacity, val -> val.map(Object::toString).orElse(""));
    }


    public MutableLiveData<Optional<Integer>> eventCapacity;
    public LiveData<String> eventCapacityDisplay() {
        return Transformations.map(eventCapacity, val -> val.map(Object::toString).orElse(""));
    }


    public MutableLiveData<Optional<Date>> lotteryDate;
    public LiveData<String> lotteryDateDisplay() {
        return dateDisplay(lotteryDate);
    }


    public MutableLiveData<Optional<Time>> lotteryTime;
    public LiveData<String> lotteryTimeDisplay() {
        return dateDisplay(lotteryDate);
    }

    // Style section field values
    private Bitmap image;
    public MutableLiveData<Optional<Color>> color;

    public CreateEventModel() {
        // TRIANGLE
        section = new MutableLiveData<>(Section.GENERAL);
        title = new MutableLiveData<>("");
        description = new MutableLiveData<>("");
        singleDayEvent = new MutableLiveData<>(true);
        eventDate = new MutableLiveData<>(Optional.empty());
        eventStartTime = new MutableLiveData<>(Optional.empty());
        eventEndTime = new MutableLiveData<>(Optional.empty());
        registrationStartDate = new MutableLiveData<>(Optional.empty());
        registrationEndDate = new MutableLiveData<>(Optional.empty());
        initialSelectionStartDate = new MutableLiveData<>(Optional.empty());
        initialSelectionEndDate = new MutableLiveData<>(Optional.empty());
        finalAttendeeSelectionDate = new MutableLiveData<>(Optional.empty());
        hasWaitlistCapacity = new MutableLiveData<>(false);
        hasLocationRequirement = new MutableLiveData<>(false);
        waitlistCapacity = new MutableLiveData<>(Optional.empty());
        eventCapacity = new MutableLiveData<>(Optional.empty());
        lotteryDate = new MutableLiveData<>(Optional.empty());
        lotteryTime = new MutableLiveData<>(Optional.empty());
        color = new MutableLiveData<>(Optional.empty());
    }

    public LiveData<Section> getSection() {
        return section;
    }

    public int getLeftButtonVisibility() {
        if (section.getValue() != Section.GENERAL) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    public int getRightButtonVisibility() {
        return View.VISIBLE;
    }

    public String getRightButtonText() {
        if (section.getValue() == Section.STYLE) return "Create";
        else return "Next";
    }

    public void nextSection() {
        Section current = section.getValue();
        if (current == Section.lastSection) {
            return;
        } else {
            section.setValue(Section.values()[current.ordinal() + 1]);
        }
    }

    public void prevSection() {
        Section current = section.getValue();
        if (current== Section.firstSection) {
            return;
        } else {
            section.setValue(Section.values()[current.ordinal() - 1]);
        }
    }

    /**
     * This is a wrapper for to build an event using the data in this model and create that event
     * using a provided {@code EventRepository}.
     * @param eventRepository A firestore database.
     * @return UUID of the create event.
     */
    public String createEvent(EventRepository eventRepository) throws NoSuchElementException {
        Event event = new Event.Builder()
                .title(title.getValue())
                .description(description.getValue())
                .eventStartDate(eventDate.getValue().orElseThrow())
                .eventStartTime(eventStartTime.getValue().orElseThrow())
                .eventEndTime(eventEndTime.getValue().orElseThrow())
                .registrationStartDate(registrationStartDate.getValue().orElseThrow())
                .registrationEndDate(registrationEndDate.getValue().orElseThrow())
                .initialSelectionStartDate(initialSelectionStartDate.getValue().orElseThrow())
                .initialSelectionEndDate(initialSelectionEndDate.getValue().orElseThrow())
                .finalSelectionDate(finalAttendeeSelectionDate.getValue().orElseThrow())
                .waitlistCapacity(waitlistCapacity.getValue())
                .requireLocation(hasLocationRequirement.getValue())
                .eventCapacity(eventCapacity.getValue().orElseThrow())
                .lotteryDate(lotteryDate.getValue().orElseThrow())
                .lotteryTime(lotteryTime.getValue().orElseThrow())
                .build();

        return eventRepository.createEvent(event);
    }
}