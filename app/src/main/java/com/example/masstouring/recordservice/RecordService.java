package com.example.masstouring.recordservice;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.mapactivity.IMapActivityCallback;
import com.example.masstouring.mapactivity.MapActivity;
import com.example.masstouring.mapactivity.MapActivityReceiver;
import com.example.masstouring.mapactivity.RecordObject;
import com.example.masstouring.mapactivity.RecordState;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import java.time.LocalDateTime;

public class RecordService extends Service implements IMapActivityCallback {
    private FusedLocationProviderClient oFusedClient;
    private SettingsClient oSetClient;
    private LocationSettingsRequest oLocSetReq;
    private LocationCallback oLocCallback;
    private LocationRequest oLocReq;
    private NotificationChannel oNotificationChannel;
    private Notification oNotification;
    private MapActivityReceiver oMapActivityReceiver;
    private RecordObject oRecordObject = null;
    private RecordState oRecordState = RecordState.STOP;
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent intent = new Intent(this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        oNotification = new Notification.Builder(this, Const.RECORD_SERVICE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getText(R.string.notificationTitle))
                .setContentText(getText(R.string.notificationContent))
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_light, getString(R.string.openMap), pendingIntent)
                .build();
        oNotificationChannel = new NotificationChannel(Const.RECORD_SERVICE_NOTIFICATION_CHANNEL_ID, getText(R.string.notificationTitle), NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(oNotificationChannel);
        initializeGpsSetting();
        initializeReceiver();
        startForeground(1, oNotification);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onCreate RecordService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LoggerTag.SYSTEM_PROCESS, "onStartCommand RecordService");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return START_STICKY;
        }
        oFusedClient.requestLocationUpdates(oLocReq, oLocCallback, Looper.myLooper());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        unregisterReceiver(oMapActivityReceiver);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onDestroy RecordService");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        oFusedClient.removeLocationUpdates(oLocCallback);
        stopForeground(true);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onTaskRemoved RecordService");
    }

    private void initializeGpsSetting(){
        oFusedClient = LocationServices.getFusedLocationProviderClient(this);
        oSetClient = LocationServices.getSettingsClient(this);
        oLocCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                Intent i = new Intent(Const.LOCATION_UPDATE_ACTION_ID);
                i.putExtra(Const.LOCATION_KEY, location);
                boolean needUpdate = false;
                if(oRecordState == RecordState.RECORDING){
                    needUpdate = oRecordObject.isDifferenceEnough(location);
                }
                i.putExtra(Const.UPDATE_KEY, needUpdate);
                if (needUpdate) {
                    oRecordObject.addLocation(location);
                    oRecordObject.inclementRecordNumber();
                    oDatabaseHelper.recordPositions(oRecordObject);
                }
                sendBroadcast(i);
                Log.d(LoggerTag.BROADCAST_PROCESS, "RecordService Sent Location Updates");
            }
        };

        oLocReq = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500);

        oLocSetReq = new LocationSettingsRequest.Builder()
                .addLocationRequest(oLocReq)
                .build();
    }

    private void initializeReceiver(){
        oMapActivityReceiver = new MapActivityReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.START_STOP_ACTION_ID);
        filter.addAction(Const.REQUEST_CURRENT_STATE_ACTION_ID);
        registerReceiver(oMapActivityReceiver, filter);
    }

    @Override
    public void onReceiveStartRecording() {
        if(oRecordObject == null) {
            oRecordObject = new RecordObject(oDatabaseHelper);
            oDatabaseHelper.recordStartInfo(oRecordObject);
            oRecordState = RecordState.RECORDING;
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "RecordService Received Start Recording");
    }

    @Override
    public void onReceiveStopRecording() {
        if(oRecordObject != null) {
            String endDate = LocalDateTime.now().format(Const.DATE_FORMAT);
            oRecordObject.setEndDate(endDate);
            oDatabaseHelper.recordEndInfo(oRecordObject);
            oRecordObject = null;
            oRecordState = RecordState.STOP;
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "RecordService Received Stop Recording");
    }

    @Override
    public void onReceiveCurrentStateRequest() {
        Intent i = new Intent(Const.REPLY_CURRENT_STATE_ACTION_ID);
        i.putExtra(Const.CURRENT_STATE, oRecordState.getId());

        if(oRecordState == RecordState.RECORDING) {
            i.putExtra(Const.RECORDING_ID, oRecordObject.getRecordId());
        }
        sendBroadcast(i);
        Log.d(LoggerTag.BROADCAST_PROCESS, "RecordService Sent Current State Reply:" + oRecordState);
    }
}
