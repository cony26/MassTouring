package com.example.masstouring.mapactivity;

import android.location.Location;

public interface IMapActivityCallback {
    public abstract void onReceiveStartRecording();
    public abstract void onReceiveStopRecording();
    public abstract void onReceiveCurrentStateRequest();
}
