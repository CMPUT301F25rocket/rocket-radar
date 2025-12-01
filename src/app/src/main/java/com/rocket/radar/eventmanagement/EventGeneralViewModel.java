package com.rocket.radar.eventmanagement;

import android.net.Uri;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.rocket.radar.uml.UmlAssociate;
import com.rocket.radar.uml.UmlCompose;
import com.rocket.radar.uml.UmlNavigate;

import java.util.ArrayList;
import java.util.Optional;

/**
 * ViewModel for managing the general details of an event during creation or editing.
 * This ViewModel holds LiveData for all the basic event properties such as title,
 * description, tagline, banner image, location, and categories.
 */
public class EventGeneralViewModel extends ViewModel {
    @UmlAssociate(selfCard = "1", label = "stores input data", otherCard = "1")
    private EventGeneralFragment eventGeneralFragment;

    @UmlCompose(selfCard = "1", label = "stores", otherCard = "1")
    private Section section;

    @UmlNavigate(selfCard = "1", label = "changes", otherCard = "1")
    private Section currentSection;

    /** LiveData holding the event title */
    public MutableLiveData<String> title;
    /** LiveData holding the event description */
    public MutableLiveData<String> description;
    /** LiveData holding the event tagline */
    public MutableLiveData<String> tagline;
    /** LiveData holding the optional banner image URI */
    public MutableLiveData<Optional<Uri>> image;
    /** LiveData holding the latitude of the event location */
    public MutableLiveData<Double> locationLatitude;
    /** LiveData holding the longitude of the event location */
    public MutableLiveData<Double> locationLongitude;
    /** LiveData holding the name of the event location */
    public MutableLiveData<String> locationName;
    /** LiveData holding the list of event categories */
    public MutableLiveData<ArrayList<String>> categories;

    /**
     * Constructs a new EventGeneralViewModel with default empty values.
     */
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
