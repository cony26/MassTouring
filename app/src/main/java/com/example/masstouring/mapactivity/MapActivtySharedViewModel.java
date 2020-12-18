package com.example.masstouring.mapactivity;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MapActivtySharedViewModel extends ViewModel {
    private MutableLiveData<Boolean> oIsTracePosition = new MutableLiveData<>();

    public MutableLiveData<Boolean> getIsTracePosition() {
        return oIsTracePosition;
    }
}
