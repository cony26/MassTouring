package com.example.masstouring.mapactivity;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

public interface IItemClickCallback {
    public abstract void onRecordItemClick(Map<Integer, LatLng> aLocationMap, Map<Integer, Double> aSpeedKmphMap);
    public abstract void onRecordItemLongClick();
}
