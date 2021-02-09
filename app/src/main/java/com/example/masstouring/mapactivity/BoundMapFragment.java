package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.recordservice.ILocationUpdateCallback;
import com.example.masstouring.recordservice.RecordService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class BoundMapFragment implements OnMapReadyCallback, LifecycleObserver, ILocationUpdateCallback {
    private GoogleMap oMap;
    private SupportMapFragment oMapFragment;
    private MapActivtySharedViewModel aMapActivityViewModel;
    private Polyline oLastPolyline = null;
    private PolylineOptions oPolylineOptions = null;
    private final DatabaseHelper oDatabaseHelper;

    public BoundMapFragment(LifecycleOwner aLifeCycleOwner, SupportMapFragment aMapFragment){
        aMapFragment.getMapAsync(this);
        oMapFragment = aMapFragment;
        aMapActivityViewModel = new ViewModelProvider(aMapFragment.getActivity()).get(MapActivtySharedViewModel.class);
        aLifeCycleOwner.getLifecycle().addObserver(this);
        oDatabaseHelper = new DatabaseHelper(aMapFragment.getContext(), Const.DB_NAME);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        oMap = googleMap;
        if(ActivityCompat.checkSelfPermission(oMapFragment.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(oMapFragment.getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        oMap.setMyLocationEnabled(true);
        oMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                aMapActivityViewModel.getIsTracePosition().setValue(true);
                return false;
            }
        });
        oMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if(i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                    aMapActivityViewModel.getIsTracePosition().setValue(false);
                }
            }
        });
    }

    public GoogleMap getMap() {
        return oMap;
    }

    @Override
    public void onReceiveLocationUpdate(Location aLocation, boolean aNeedUpdate) {
        if(aMapActivityViewModel.getIsTracePosition().getValue()) {
            oMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()), 16f));
        }

        if(!aNeedUpdate)
            return;

        if(aMapActivityViewModel.isRecording()) {
            if(oLastPolyline != null){
                oLastPolyline.remove();
            }
            oPolylineOptions.add(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()));
            oLastPolyline = oMap.addPolyline(oPolylineOptions);
        }
        Log.d(LoggerTag.SYSTEM_PROCESS, "Location Updates");
    }

    public void moveCameraIfRecording(RecordService aService){
        if(aMapActivityViewModel.isRecording()){
            int id = aService.getRecordObject().getRecordId();
            oPolylineOptions = oDatabaseHelper.restorePolylineOptionsFrom(id);
            oLastPolyline = oMap.addPolyline(oPolylineOptions);
            oDatabaseHelper.getLastLatLngFrom(id).ifPresent(e -> oMap.moveCamera(CameraUpdateFactory.newLatLngZoom(e, 16f)));
        }
    }

    public void initialize(){
        oMap.clear();
        oPolylineOptions = new PolylineOptions();
    }

    public void drawMarkers(List<Picture> aPictureList){
        for(Picture picture : aPictureList){
            oMap.addMarker(new MarkerOptions()
                    .position(picture.getLatLng())
                    .icon(BitmapDescriptorFactory.fromBitmap(picture.getBitmap())));
        }
    }
}
