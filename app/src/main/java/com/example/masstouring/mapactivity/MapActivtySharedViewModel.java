package com.example.masstouring.mapactivity;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.masstouring.common.Const;
import com.example.masstouring.database.DatabaseHelper;

public class MapActivtySharedViewModel extends ViewModel {
    private MutableLiveData<Boolean> oIsTracePosition = new MutableLiveData<>(true);
    private MutableLiveData<RecordState> oRecordState = new MutableLiveData<>(RecordState.STOP);
    private MutableLiveData<Boolean> oIsRecordsViewVisible = new MutableLiveData<>(false);

    public MutableLiveData<Boolean> getIsTracePosition() {
        return oIsTracePosition;
    }
    public MutableLiveData<RecordState> getRecordState() {
        return oRecordState;
    }
    public MutableLiveData<Boolean> getIsRecordsViewVisible(){return oIsRecordsViewVisible;}
}
