package com.example.masstouring.mapactivity;

import android.view.View;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class MapActivtySharedViewModel extends ViewModel {
    private MutableLiveData<Boolean> oIsTracePosition = new MutableLiveData<>(true);
    private MutableLiveData<RecordState> oRecordState = new MutableLiveData<>(RecordState.STOP);
    private MutableLiveData<RecordStartEvent> oRecordStartEvent = new MutableLiveData<>();
    private MutableLiveData<RecordEndEvent> oRecordEndEvent = new MutableLiveData<>();
    private MutableLiveData<Boolean> oIsRecordsViewVisible = new MutableLiveData<>(false);
    private MutableLiveData<Integer> oToolbarVisiblity = new MutableLiveData<>(View.GONE);

    public MutableLiveData<Boolean> getIsTracePosition() {
        return oIsTracePosition;
    }
    public MutableLiveData<RecordState> getRecordState() {
        return oRecordState;
    }
    public boolean isRecording(){
        return oRecordState.getValue().equals(RecordState.RECORDING);
    }
    public MutableLiveData<Boolean> getIsRecordsViewVisible(){return oIsRecordsViewVisible;}
    public MutableLiveData<Integer> getToolbarVisibility(){return oToolbarVisiblity;}

    public MutableLiveData<RecordStartEvent> getRecordStartEvent() {
        return oRecordStartEvent;
    }

    public MutableLiveData<RecordEndEvent> getRecordEndEvent(){
        return oRecordEndEvent;
    }
}
