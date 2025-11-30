package com.rocket.radar.eventmanagement;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EventGeneralViewModel extends ViewModel {
    public MutableLiveData<String> title;
    public MutableLiveData<String> description;
    public MutableLiveData<String> tagline;

    public EventGeneralViewModel() {
        title = new MutableLiveData<>("");
        description = new MutableLiveData<>("");
        tagline = new MutableLiveData<>("");
    }
}
