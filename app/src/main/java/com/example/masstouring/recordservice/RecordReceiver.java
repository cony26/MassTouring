package com.example.masstouring.recordservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.example.masstouring.common.Const;

public class RecordReceiver extends BroadcastReceiver {
    private final ILocationUpdateCallback oCallback;

    public RecordReceiver(ILocationUpdateCallback aCallback){
        oCallback = aCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        Location loc = (Location)data.getParcelable(Const.LOCATION_KEY);
        oCallback.onReceiveLocationUpdate(loc);
    }
}
