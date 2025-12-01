package com.rocket.radar.eventmanagement;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Optional;

public class EventGeneralViewModel extends ViewModel {
    public MutableLiveData<String> title;
    public MutableLiveData<String> description;
    public MutableLiveData<String> tagline;
    public MutableLiveData<Optional<Uri>> image;
    public MutableLiveData<Double> locationLatitude;
    public MutableLiveData<Double> locationLongitude;
    public MutableLiveData<String> locationName;
    public MutableLiveData<ArrayList<String>> categories;

    public EventGeneralViewModel() {
        title = new MutableLiveData<>("");
        description = new MutableLiveData<>("");
        tagline = new MutableLiveData<>("");
        image = new MutableLiveData<>(Optional.empty());
        locationLatitude = new MutableLiveData<>(null);
        locationLongitude = new MutableLiveData<>(null);
        locationName = new MutableLiveData<>("");
        categories = new MutableLiveData<>(new ArrayList<>());
    }
}
