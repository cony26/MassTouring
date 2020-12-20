package com.example.masstouring.recordservice;

import android.location.Location;

import com.example.masstouring.mapactivity.RecordState;

public interface IRecordServiceCallback {
    public abstract void onReceiveLocationUpdate(Location aLocation, boolean aNeedUpdate);
}
