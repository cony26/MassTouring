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
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.mapactivity.MapActivity;
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
import java.util.Optional;

public class RecordService extends Service{
    private FusedLocationProviderClient oFusedClient;
    private SettingsClient oSetClient;
    private LocationSettingsRequest oLocSetReq;
    private LocationCallback oLocCallback;
    private LocationRequest oLocReq;
    private NotificationChannel oNotificationChannel;
    private Notification oNotification;
    private RecordObject oRecordObject = null;
    private RecordState oRecordState = RecordState.STOP;
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);
    private static final String CANCEL_ACTION = "cancel record action";
    private final IBinder oBinder = new RecordServiceBinder();
    private Optional<IRecordServiceCallback> oRecordServiceCallback = Optional.empty();

    public class RecordServiceBinder extends Binder {
        public RecordService getRecordService(){
            return RecordService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LoggerTag.SYSTEM_PROCESS, "onBind RecordService");
        return oBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent openMapIntent = new Intent(this, MapActivity.class);
        openMapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openMapPendingIntent = PendingIntent.getActivity(this, 0, openMapIntent, 0);
        Intent cancelRecordIntent = new Intent(this, RecordService.class);
        cancelRecordIntent.setAction(CANCEL_ACTION);
        PendingIntent cancelRecordPendingIntent = PendingIntent.getForegroundService(this, 0, cancelRecordIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        oNotification = new NotificationCompat.Builder(this, Const.RECORD_SERVICE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getText(R.string.notificationTitle))
                .setContentText(getText(R.string.notificationContent))
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .setContentIntent(openMapPendingIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_light, getString(R.string.openMap), openMapPendingIntent)
                .addAction(R.drawable.common_google_signin_btn_icon_dark, getString(R.string.notificationStopAction), cancelRecordPendingIntent)
                .build();
        oNotificationChannel = new NotificationChannel(Const.RECORD_SERVICE_NOTIFICATION_CHANNEL_ID, getText(R.string.notificationTitle), NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(oNotificationChannel);
        initializeGpsSetting();
        startForeground(1, oNotification);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onCreate RecordService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LoggerTag.SYSTEM_PROCESS, "onStartCommand RecordService");
        Optional.ofNullable(intent.getAction()).ifPresent(e -> {
            if(e.equals(CANCEL_ACTION)){
                //TODO:this stopSelf doesn't work correctly. It may need to unbind this service in activity before calling this method
                stopSelf();
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            return START_STICKY;
        }
        oFusedClient.requestLocationUpdates(oLocReq, oLocCallback, Looper.myLooper());
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onDestroy RecordService");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopService();
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
                    i.putExtra(Const.UPDATE_KEY, needUpdate);
                }

                if (needUpdate) {
                    oRecordObject.addLocation(location);
                    oRecordObject.inclementRecordNumber();
                    oDatabaseHelper.recordPositions(oRecordObject);
                }

//                sendBroadcast(i);
                boolean finalNeedUpdate = needUpdate;
                oRecordServiceCallback.ifPresent(callback -> callback.onReceiveLocationUpdate(location, finalNeedUpdate));
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

    public void startRecording() {
        if(oRecordState == RecordState.STOP) {
            oRecordObject = new RecordObject(oDatabaseHelper);
            oDatabaseHelper.recordStartInfo(oRecordObject);
            oRecordState = RecordState.RECORDING;
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "RecordService Received Start Recording");
    }

    public void stopRecording() {
        if(oRecordState == RecordState.RECORDING) {
            String endDate = LocalDateTime.now().format(Const.DATE_FORMAT);
            oRecordObject.setEndDate(endDate);
            oDatabaseHelper.recordEndInfo(oRecordObject);
            oRecordObject = null;
            oRecordState = RecordState.STOP;
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "RecordService Received Stop Recording");
    }

    private void stopService(){
        stopRecording();
        //TODO:need to report stop to Activity (RecordState)
        oFusedClient.removeLocationUpdates(oLocCallback);
        stopForeground(true);
    }

    public RecordState getRecordState(){
        return oRecordState;
    }

    public void setIRecordServiceCallback(IRecordServiceCallback aCallback){
        oRecordServiceCallback = Optional.ofNullable(aCallback);
    }

    public RecordObject getRecordObject(){
        return oRecordObject;
    }
}
