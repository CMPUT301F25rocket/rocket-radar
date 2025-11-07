package com.rocket.radar.events;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.Date;

public class FilterModel extends ViewModel {
    // class should contain list of filters currenty aypplied (and later the date selectd)
    private MutableLiveData<ArrayList<String>> filters = new MutableLiveData<>(new ArrayList<>());
    private MutableLiveData<Date> date = new MutableLiveData<>(null);


    public LiveData<ArrayList<String>> getFilters() {
        return filters;
    }

    public LiveData<Date> getDate() {
        return date;
    }

    public void setFilters(ArrayList<String> filters) {
        this.filters.setValue(filters);
    }

    public void setDate(Date date) {
        this.date.setValue(date);
    }
}
