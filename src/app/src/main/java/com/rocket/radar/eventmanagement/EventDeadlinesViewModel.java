package com.rocket.radar.eventmanagement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

public class EventDeadlinesViewModel extends ViewModel {
    private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd");

    public MutableLiveData<Optional<Date>> registrationStartDate;
    public MutableLiveData<Optional<Date>> initialSelectionStartDate;
    public MutableLiveData<Optional<Date>> finalAttendeeSelectionDate;

    public EventDeadlinesViewModel() {
        registrationStartDate = new MutableLiveData<>(Optional.empty());
        initialSelectionStartDate = new MutableLiveData<>(Optional.empty());
        finalAttendeeSelectionDate = new MutableLiveData<>(Optional.empty());
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
