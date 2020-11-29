package com.example.masstouring.mapactivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.masstouring.common.Const;

public class MapActivityReceiver extends BroadcastReceiver{
    private final IMapActivityCallback oCallback;

    public MapActivityReceiver(IMapActivityCallback aCallback){
        oCallback = aCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionId = intent.getAction();
        if(actionId.equals(Const.START_STOP_ACTION_ID)) {
            String info = intent.getStringExtra(Const.START_STOP_RECORDING_KEY);
            if (info.equals(Const.START_RECORDING)) {
                oCallback.onReceiveStartRecording();
            } else if (info.equals(Const.STOP_RECORDING)) {
                oCallback.onReceiveStopRecording();
            }
        }else if(actionId.equals(Const.REQUEST_CURRENT_STATE_ACTION_ID)){
            oCallback.onReceiveCurrentStateRequest();
        }
    }
}
