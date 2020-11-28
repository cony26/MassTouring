package com.example.masstouring.recordservice;

import android.location.Location;

public interface ILocationUpdateCallback {
    public abstract void onReceiveLocationUpdate(Location aLocation);
}
