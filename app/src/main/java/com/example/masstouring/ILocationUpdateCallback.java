package com.example.masstouring;

import android.location.Location;

public interface ILocationUpdateCallback {
    public abstract void onReceiveLocationUpdate(Location aLocation);
}
