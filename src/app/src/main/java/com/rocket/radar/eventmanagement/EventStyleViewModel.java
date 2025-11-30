package com.rocket.radar.eventmanagement;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Optional;

public class EventStyleViewModel extends ViewModel {
    public MutableLiveData<Optional<Uri>> image;

    public EventStyleViewModel() {
        image = new MutableLiveData<>(Optional.empty());
    }
}
