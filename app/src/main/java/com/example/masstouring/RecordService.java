package com.example.masstouring;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

public class RecordService extends Service {
    private FusedLocationProviderClient oFusedClient;
    private SettingsClient oSetClient;
    private LocationSettingsRequest oLocSetReq;
    private LocationCallback oLocCallback;
    private LocationRequest oLocReq;
    private NotificationChannel oNotificationChannel;
    private Notification oNotification;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
    public void onCreate() {
        super.onCreate();
        oNotificationChannel = new NotificationChannel(Const.RECORD_SERVICE_NOTIFICATION_CHANNEL_ID, getText(R.string.notificationTitle), NotificationManager.IMPORTANCE_DEFAULT);
        oNotification = new Notification.Builder(this, Const.RECORD_SERVICE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getText(R.string.notificationTitle))
                .setContentText(getText(R.string.notificationContent))
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_dark)
                .build();
        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(oNotificationChannel);
        initializeGpsSetting();
        startForeground(1, oNotification);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onCreate RecordService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onDestroy RecordService");
    }

    private void initializeGpsSetting(){
        oFusedClient = LocationServices.getFusedLocationProviderClient(this);
        oSetClient = LocationServices.getSettingsClient(this);
        oLocCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location loc = locationResult.getLastLocation();
                Intent i = new Intent(Const.RECORD_SERVICE_ACTION_ID);
                i.putExtra(Const.LOCATION_KEY, loc);

                sendBroadcast(i);
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

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopForeground(true);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onTaskRemoved RecordService");
    }
}
