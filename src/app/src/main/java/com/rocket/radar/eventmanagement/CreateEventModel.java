package com.rocket.radar.eventmanagement;import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;

// REMOVE BaseObservable and Bindable imports
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import java.util.Date;

// Your class should only extend ViewModel
public class CreateEventModel extends ViewModel {
    // Other state
    private boolean showBottomSheet;

    public boolean getShowBottomSheet() {
        return showBottomSheet;
    }

    // Creation wizard state.
    private MutableLiveData<Section> section;

    // General section field values
    public MutableLiveData<String> title = new MutableLiveData<>();
    public MutableLiveData<String> description = new MutableLiveData<>();


    // Datetime section field values
    public MutableLiveData<Boolean> singleDayEvent = new MutableLiveData<>();
    private MutableLiveData<Date> eventDate = new MutableLiveData<>();
    private Time eventStartTime;
    private Time eventEndTime;

    // ... (rest of your fields)

    // Style section field values
    private Bitmap image;
    private Color color;

    public CreateEventModel() {
        section = new MutableLiveData<>(Section.GENERAL);
    }

    // No @Bindable needed here
    public LiveData<Section> getSection() {
        return section;
    }

    // No @Bindable. Use Transformations.map to create a LiveData that reacts to changes in 'section'.
    public LiveData<Integer> getLeftButtonVisibility() {
        return Transformations.map(section, currentSection -> {
            if (currentSection != Section.GENERAL) {
                return View.GONE;
            } else {
                return View.VISIBLE;
            }
        });
    }

    // No @Bindable. Visibility is always visible, so this can be a simple LiveData.
    public LiveData<Integer> getRightButtonVisibility() {
        // Since this is always visible, you can just return a static LiveData
        // or keep it simple if your XML doesn't bind to it dynamically.
        // For consistency, here is the reactive way:
        return new MutableLiveData<>(View.VISIBLE);
    }

    // No @Bindable. Use Transformations.map for reactive text changes.
    public LiveData<String> getRightButtonText() {
        return Transformations.map(section, currentSection -> {
            if (currentSection == Section.STYLE) return "Create";
            else return "Next";
        });
    }

    public void nextSection() {
        Section current = section.getValue();
        // Assuming Section.lastSection is a valid concept in your enum
        if (current != null && current.ordinal() < Section.values().length - 1) {
            section.setValue(Section.values()[current.ordinal() + 1]);
        }
    }

    public void prevSection() {
        Section current = section.getValue();
        // Assuming Section.firstSection is a valid concept in your enum
        if (current != null && current.ordinal() > 0) {
            section.setValue(Section.values()[current.ordinal() - 1]);
        }
    }

    // You would also need your Section enum defined somewhere, possibly inside this class or in its own file.
    public enum Section {
        GENERAL, DATETIME, DEADLINE, LOTTERY, STYLE // Example sections
    }
}
