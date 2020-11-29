package com.example.masstouring.mapactivity;

public enum RecordState {
    RECORDING(0),
    STOP(1);

    int oId;
    RecordState(int aId){
        oId = aId;
    }

    public int getId(){
        return oId;
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
