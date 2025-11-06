package com.rocket.radar.eventmanagement;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.rocket.radar.events.Event;
import com.rocket.radar.events.EventRepository;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    public LiveData<String> eventStartTimeDisplay() {
        return Transformations.map(eventStartTime, time -> time.map(Time::toString).orElse(""));
    }

    public MutableLiveData<Optional<Time>> eventEndTime;
    public LiveData<String> eventEndTimeDisplay() {
        return Transformations.map(eventEndTime, time -> time.map(Time::toString).orElse(""));
    }

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
    public MutableLiveData<Optional<Uri>> image;
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
        // Actually be default this should be some random cover image.
        image = new MutableLiveData<>(Optional.empty());
        color = new MutableLiveData<>(Optional.empty());
    }

    public LiveData<Section> getSection() {
        return section;
    }

    public LiveData<Integer> getLeftButtonVisibility() {
        return Transformations.map(section, s -> {
            if (s == Section.GENERAL) {
                return View.GONE;
            } else {
                return View.VISIBLE;
            }
        });
    }

    public LiveData<Integer> getRightButtonVisibility() {
        return Transformations.map(section, s -> {
            return View.VISIBLE;
        });
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
    public String createEvent(ContentResolver contentResolver, EventRepository eventRepository) throws Exception {
        Uri bannerImageUri = image.getValue().orElseThrow(
            () -> new NoSuchElementException("The banner image is missing")
        );

        Bitmap bitmap;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(contentResolver, bannerImageUri);
        } catch (FileNotFoundException e) {
            throw new Exception("Provided image could not be read from storage");
        }


        // NOTE: Had ChatGPT fill in the NoSuchElementExceptions. Was too lazy.
        Event event = new Event.Builder()
            .title(title.getValue())
            .description(description.getValue())
            .eventStartDate(eventDate.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The event start date is missing.")))
            .eventStartTime(eventStartTime.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The event start time is missing.")))
            .eventEndTime(eventEndTime.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The event end time is missing.")))
            .registrationStartDate(registrationStartDate.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The registration start date is missing.")))
            .registrationEndDate(registrationEndDate.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The registration end date is missing.")))
            .initialSelectionStartDate(initialSelectionStartDate.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The initial selection start date is missing.")))
            .initialSelectionEndDate(initialSelectionEndDate.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The initial selection end date is missing.")))
            .finalSelectionDate(finalAttendeeSelectionDate.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The final selection date is missing.")))
            .waitlistCapacity(waitlistCapacity.getValue())
            .requireLocation(hasLocationRequirement.getValue())
            .eventCapacity(eventCapacity.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The event capacity is missing.")))
            .lotteryDate(lotteryDate.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The lottery date is missing.")))
            .lotteryTime(lotteryTime.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The lottery time is missing.")))
            .bannerImage(bitmap)
            .color(color.getValue()
                    .orElseThrow(() -> new NoSuchElementException("The event color is missing.")))
            .build();

        return eventRepository.createEvent(event);
    }
}