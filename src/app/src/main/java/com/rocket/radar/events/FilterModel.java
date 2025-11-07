package com.rocket.radar.events;

import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

public class FilterModel extends ViewModel {
    // class should contain list of filters currenty aypplied (and later the date selectd)
    private ArrayList<String> filters;

    public ArrayList<String> getFilters() {
        return filters;
    }

    public void setFilters(ArrayList<String> filters) {
        this.filters = filters;
    }



}
