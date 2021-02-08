package com.example.masstouring.mapactivity;

import com.google.android.gms.maps.model.LatLng;

import java.util.Map;

public interface IItemClickCallback {
    public abstract void onRecordItemClick(RecordItem aRecordItem);
    public abstract void onRecordItemLongClick();
}
