package com.example.masstouring.mapactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class MapActivityReceiver extends BroadcastReceiver{
    private final IMapActivityCallback oCallback;

    public MapActivityReceiver(IMapActivityCallback aCallback){
        oCallback = aCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();
        oCallback.onReceiveInformation();
    }
}
