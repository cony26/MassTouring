package com.example.masstouring.recordservice;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class BoundLocationClient implements LifecycleObserver{
    private FusedLocationProviderClient oFusedClient;
    private LocationCallback oLocCallback;
    private final Context oContext;

    public BoundLocationClient(LifecycleOwner aOwner, Context aContext, LocationCallback aCallback){
        aOwner.getLifecycle().addObserver(this);
        oContext = aContext;
        oLocCallback = aCallback;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private void initializeLocationClient(){
        SettingsClient setClient = LocationServices.getSettingsClient(oContext);
        oFusedClient = LocationServices.getFusedLocationProviderClient(oContext);

        LocationRequest locReq = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(Const.UPDATE_INTERVAL)
                .setFastestInterval(Const.UPDATE_FASTEST_INTERVAL);

        LocationSettingsRequest oLocSetReq = new LocationSettingsRequest.Builder()
                .addLocationRequest(locReq)
                .build();

        setClient.checkLocationSettings(oLocSetReq)
                .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(oContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                            return;

                        oFusedClient.requestLocationUpdates(locReq, oLocCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(LoggerTag.LOCATION, "failed to set up location settings");
                    }
                });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void removeClient(){
        oFusedClient.removeLocationUpdates(oLocCallback);
    }
}
