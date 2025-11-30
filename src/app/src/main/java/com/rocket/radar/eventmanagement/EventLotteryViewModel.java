package com.rocket.radar.eventmanagement;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Optional;

public class EventLotteryViewModel extends ViewModel {
    public MutableLiveData<Boolean> hasWaitlistCapacity;
    public MutableLiveData<Boolean> hasLocationRequirement;
    public MutableLiveData<Optional<Integer>> waitlistCapacity;
    public MutableLiveData<Optional<Integer>> eventCapacity;

    public EventLotteryViewModel() {
        hasWaitlistCapacity = new MutableLiveData<>(false);
        hasLocationRequirement = new MutableLiveData<>(false);
        waitlistCapacity = new MutableLiveData<>(Optional.empty());
        eventCapacity = new MutableLiveData<>(Optional.empty());
    }

    public LiveData<String> waitlistCapacityDisplay() {
        return Transformations.map(waitlistCapacity, val -> val.map(Object::toString).orElse(""));
    }

    public LiveData<String> eventCapacityDisplay() {
        return Transformations.map(eventCapacity, val -> val.map(Object::toString).orElse(""));
    }
}
