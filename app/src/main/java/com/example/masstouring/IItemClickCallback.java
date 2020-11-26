package com.example.masstouring;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

public interface IItemClickCallback {
    public abstract void onRecordItemClick(Map<Integer, LatLng> aMap);
}
