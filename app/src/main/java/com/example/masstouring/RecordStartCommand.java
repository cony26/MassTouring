package com.example.masstouring;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.Serializable;
import java.time.LocalDateTime;

public class RecordStartCommand implements Serializable {
    private final FusedLocationProviderClient oFusedClient;
    private final LocationCallback oLocCallback;
    private final LocationRequest oLocReq;

    public RecordStartCommand(FusedLocationProviderClient aFusedClient, LocationCallback aLocCallback, LocationRequest aLocReq){
        oFusedClient = aFusedClient;
        oLocCallback = aLocCallback;
        oLocReq = aLocReq;
    }

    public void execute(){
        oFusedClient.requestLocationUpdates(oLocReq, oLocCallback, Looper.myLooper());
    }
}
