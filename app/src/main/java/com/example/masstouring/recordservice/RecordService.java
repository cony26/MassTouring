package com.example.masstouring.recordservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.mapactivity.MapActivity;
import com.example.masstouring.mapactivity.RecordObject;
import com.example.masstouring.mapactivity.RecordState;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import java.time.LocalDateTime;
import java.util.Optional;

public class RecordService extends LifecycleService {
    private BoundLocationClient oBoundLocationClient;
    private LocationCallback oLocCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Location location = locationResult.getLastLocation();

            boolean needUpdate = false;
            if(oRecordState == RecordState.RECORDING){
                needUpdate = oRecordObject.isDifferenceEnough(location);
            }

            if (needUpdate) {
                oRecordObject.addLocation(location);
                oRecordObject.inclementRecordNumber();
                oDatabaseHelper.recordPositions(oRecordObject);
            }

            boolean finalNeedUpdate = needUpdate;
            oRecordServiceCallback.ifPresent(callback -> callback.onReceiveLocationUpdate(location, finalNeedUpdate));
        }
    };

    private NotificationChannel oNotificationChannel;
    private Notification oNotification;
    private RecordObject oRecordObject = null;
    private RecordState oRecordState = RecordState.STOP;
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);
    private static final String CANCEL_ACTION = "cancel record action";
    private final IBinder oBinder = new RecordServiceBinder();
    private Optional<ILocationUpdateCallback> oRecordServiceCallback = Optional.empty();
    private Optional<IUnbindRequestCallback> oUnbindRequestCallback;

    public class RecordServiceBinder extends Binder {
        public RecordService getRecordService(){
            return RecordService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        Log.d(LoggerTag.SYSTEM_PROCESS, "onBind RecordService");
        return oBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new LifeCycleLogger(this, getClass().getSimpleName());
        oBoundLocationClient = new BoundLocationClient(this, this, oLocCallback);

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
        startForeground(1, oNotification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.i(LoggerTag.SYSTEM_PROCESS, "onStartCommand RecordService");
        Optional.ofNullable(intent.getAction()).ifPresent(e -> {
            if(e.equals(CANCEL_ACTION)){
                oUnbindRequestCallback.ifPresent(callback -> callback.unbindRecordService());
                stopSelf();
            }
        });
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopService();
        oBoundLocationClient.removeClient();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onTaskRemoved RecordService");
    }

    public void startRecording() {
        if(oRecordState == RecordState.STOP) {
            oRecordObject = new RecordObject(oDatabaseHelper);
            oDatabaseHelper.recordStartInfo(oRecordObject);
            oRecordState = RecordState.RECORDING;
        }
        Log.d(LoggerTag.RECORD_SERVICE_PROCESS, "RecordService start Recording");
    }

    public void stopRecording() {
        if(oRecordState == RecordState.RECORDING) {
            String endDate = LocalDateTime.now().format(Const.DATE_FORMAT);
            oRecordObject.setEndDate(endDate);
            oDatabaseHelper.recordEndInfo(oRecordObject);
            oRecordObject = null;
            oRecordState = RecordState.STOP;
        }
        Log.d(LoggerTag.RECORD_SERVICE_PROCESS, "RecordService stop Recording");
    }

    private void stopService(){
        stopRecording();
        stopForeground(true);
    }

    public RecordState getRecordState(){
        return oRecordState;
    }

    public void setIRecordServiceCallback(ILocationUpdateCallback aCallback){
        oRecordServiceCallback = Optional.ofNullable(aCallback);
    }

    public RecordObject getRecordObject(){
        return oRecordObject;
    }

    public void setUnbindRequestCallback(IUnbindRequestCallback aCallback){
        oUnbindRequestCallback = Optional.ofNullable(aCallback);
    }

    public interface IUnbindRequestCallback{
        void unbindRecordService();
    }
}
