package com.example.masstouring;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

public interface IItemClickCallback {
    public abstract void onClick(Map<Integer, LatLng> aMap);
}
