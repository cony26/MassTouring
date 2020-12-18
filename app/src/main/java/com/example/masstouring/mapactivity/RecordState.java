package com.example.masstouring.mapactivity;

import android.content.res.Resources;

import com.example.masstouring.R;

public enum RecordState {
    RECORDING(0, R.string.stopRecording, R.string.touringStartToast),
    STOP(1, R.string.startRecording, R.string.touringFinishToast);

    private int oId;
    private int oButtonStringId;
    private int oToastId;
    RecordState(int aId, int aButtonStringId, int aToastId){
        oId = aId;
        oButtonStringId = aButtonStringId;
        oToastId = aToastId;
    }

    public int getId(){
        return oId;
    }
    public int getButtonStringId(){
        return oButtonStringId;
    }

    public int getToastId() {
        return oToastId;
    }

    public static RecordState getCorrespondingToId(int aId){
        if(aId == RECORDING.oId){
            return RECORDING;
        }else if(aId == STOP.oId){
            return STOP;
        }

        return null;
    }

}
