package com.rocket.radar.eventmanagement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class EventDateTimeViewModel extends ViewModel {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");

    // Event date and time fields
    public MutableLiveData<Optional<Date>> eventDate;
    public MutableLiveData<Optional<Time>> eventStartTime;
    public MutableLiveData<Optional<Time>> eventEndTime;

    // Deadline fields (merged from EventDeadlinesViewModel)
    public MutableLiveData<Optional<Date>> registrationStartDate;
    public MutableLiveData<Optional<Date>> initialSelectionStartDate;
    public MutableLiveData<Optional<Date>> finalAttendeeSelectionDate;

    public EventDateTimeViewModel() {
        eventDate = new MutableLiveData<>(Optional.empty());
        eventStartTime = new MutableLiveData<>(Optional.empty());
        eventEndTime = new MutableLiveData<>(Optional.empty());
        registrationStartDate = new MutableLiveData<>(Optional.empty());
        initialSelectionStartDate = new MutableLiveData<>(Optional.empty());
        finalAttendeeSelectionDate = new MutableLiveData<>(Optional.empty());
    }

    public LiveData<String> eventDateDisplay() {
        return Transformations.map(eventDate, date -> date.map(dateFormatter::format).orElse(""));
    }

    public LiveData<String> eventStartTimeDisplay() {
        return Transformations.map(eventStartTime, time -> time.map(Time::toString).orElse(""));
    }

    public LiveData<String> eventEndTimeDisplay() {
        return Transformations.map(eventEndTime, time -> time.map(Time::toString).orElse(""));
    }

    public LiveData<String> registrationStartDateDisplay() {
        return Transformations.map(registrationStartDate, date -> date.map(dateFormatter::format).orElse(""));
    }

    public LiveData<String> initialSelectionStartDateDisplay() {
        return Transformations.map(initialSelectionStartDate, date -> date.map(dateFormatter::format).orElse(""));
    }

    public LiveData<String> finalAttendeeSelectionDateDisplay() {
        return Transformations.map(finalAttendeeSelectionDate, date -> date.map(dateFormatter::format).orElse(""));
    }
}
