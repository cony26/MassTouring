package com.example.masstouring.recordservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.example.masstouring.common.Const;
import com.example.masstouring.mapactivity.RecordState;

public class RecordReceiver extends BroadcastReceiver {
    private final IRecordServiceCallback oCallback;

    public RecordReceiver(IRecordServiceCallback aCallback){
        oCallback = aCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String actionId = intent.getAction();

        if(actionId.equals(Const.LOCATION_UPDATE_ACTION_ID)) {
            Bundle data = intent.getExtras();
            Location loc = (Location) data.getParcelable(Const.LOCATION_KEY);
            boolean needUpdate = (boolean) intent.getBooleanExtra(Const.UPDATE_KEY, false);
            oCallback.onReceiveLocationUpdate(loc, needUpdate);
        }else if(actionId.equals(Const.REPLY_CURRENT_STATE_ACTION_ID)){
            int stateId = (int)intent.getIntExtra(Const.CURRENT_STATE, 0);
            RecordState recordState = RecordState.getCorrespondingToId(stateId);

            int recordId = -1;
            if(recordState == RecordState.RECORDING){
                recordId = (int)intent.getIntExtra(Const.RECORDING_ID, -1);
            }
            oCallback.onReceiveReplyCurrentState(recordState, recordId);
        }
    }
}
