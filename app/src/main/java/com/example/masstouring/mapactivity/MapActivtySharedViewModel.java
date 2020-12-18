package com.example.masstouring.mapactivity;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MapActivtySharedViewModel extends ViewModel {
    private MutableLiveData<Boolean> oIsTracePosition = new MutableLiveData<>(true);
    private MutableLiveData<RecordState> oRecordState = new MutableLiveData<>(RecordState.STOP);

    public MutableLiveData<Boolean> getIsTracePosition() {
        return oIsTracePosition;
    }

    public MutableLiveData<RecordState> getRecordState() {
        return oRecordState;
    }
}
