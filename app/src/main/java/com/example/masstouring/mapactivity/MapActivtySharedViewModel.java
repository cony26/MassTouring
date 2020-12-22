package com.example.masstouring.mapactivity;

import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MapActivtySharedViewModel extends ViewModel {
    private MutableLiveData<Boolean> oIsTracePosition = new MutableLiveData<>(true);
    private MutableLiveData<RecordState> oRecordState = new MutableLiveData<>(RecordState.STOP);
    private MutableLiveData<Boolean> oIsRecordsViewVisible = new MutableLiveData<>(false);
    private MutableLiveData<Integer> oToolbarVisiblity = new MutableLiveData<>(View.GONE);

    public MutableLiveData<Boolean> getIsTracePosition() {
        return oIsTracePosition;
    }
    public MutableLiveData<RecordState> getRecordState() {
        return oRecordState;
    }
    public MutableLiveData<Boolean> getIsRecordsViewVisible(){return oIsRecordsViewVisible;}
    public MutableLiveData<Integer> getToolbarVisibility(){return oToolbarVisiblity;}
}