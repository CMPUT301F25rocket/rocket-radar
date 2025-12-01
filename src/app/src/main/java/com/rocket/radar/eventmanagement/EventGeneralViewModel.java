package com.rocket.radar.eventmanagement;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rocket.radar.uml.UmlAssociate;
import com.rocket.radar.uml.UmlCompose;
import com.rocket.radar.uml.UmlNavigate;

import java.util.ArrayList;
import java.util.Optional;

public class EventGeneralViewModel extends ViewModel {
    @UmlAssociate(selfCard = "1", label = "stores input data", otherCard = "1")
    private EventGeneralFragment eventGeneralFragment;

    @UmlCompose(selfCard = "1", label = "stores", otherCard = "1")
    private Section section;

    @UmlNavigate(selfCard = "1", label = "changes", otherCard = "1")
    private Section currentSection;

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
