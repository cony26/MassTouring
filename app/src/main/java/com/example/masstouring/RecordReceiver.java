package com.example.masstouring;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;

public class RecordReceiver extends BroadcastReceiver {
    private final ILocationUpdateCallback oCallback;

    public RecordReceiver(ILocationUpdateCallback aCallback){
        oCallback = aCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        Location loc = (Location)data.getParcelable("location");
        oCallback.onReceiveLocationUpdate(loc);
    }
}
