package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.recordservice.ILocationUpdateCallback;
import com.example.masstouring.recordservice.RecordService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;

import java.util.List;

public class BoundMapFragment implements OnMapReadyCallback, LifecycleObserver, ILocationUpdateCallback {
    private GoogleMap oMap;
    private ClusterManager<Picture> oClusterManager;
    private PictureClusterRenderer oPictureClusterRenderer = null;
    private SupportMapFragment oMapFragment;
    private MapActivtySharedViewModel aMapActivityViewModel;
    private Polyline oLastPolyline = null;
    private PolylineOptions oPolylineOptions = null;
    private DatabaseHelper oDatabaseHelper;

    public BoundMapFragment(LifecycleOwner aLifeCycleOwner, SupportMapFragment aMapFragment){
        Log.i(LoggerTag.SYSTEM_PROCESS, "BoundMapFragment:constructor");
        aMapFragment.getMapAsync(this);
        oMapFragment = aMapFragment;
        oMapFragment.getLifecycle().addObserver(this);
        new LifeCycleLogger(oMapFragment.getViewLifecycleOwner(), oMapFragment.getClass().getSimpleName());
        aMapActivityViewModel = new ViewModelProvider(aMapFragment.getActivity()).get(MapActivtySharedViewModel.class);
        oDatabaseHelper = new DatabaseHelper(aMapFragment.getContext(), Const.DB_NAME);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i(LoggerTag.SYSTEM_PROCESS, "BoundMapFragment:onMapReady");
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
        instantiateClusterManagers();
        oMap.clear();
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
        oClusterManager.getMarkerCollection().clear();
        oClusterManager.clearItems();
        oMap.clear();
        oPolylineOptions = new PolylineOptions();
    }

    public void drawMarkers(List<Picture> aPictureList){
        for(Picture picture : aPictureList){
            oClusterManager.addItem(picture);
        }
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                oClusterManager.cluster();
            }
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(){
        if(oMap != null){
            instantiateClusterManagers();
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void clearAll(){
        oClusterManager.clearItems();
        oClusterManager.cluster();
        oPictureClusterRenderer.setOnClusterClickListener(null);
        oPictureClusterRenderer.setOnClusterItemClickListener(null);
        oClusterManager.getMarkerCollection().clear();
        oPictureClusterRenderer.onRemove();
    }

    private void instantiateClusterManagers(){
        oClusterManager = new ClusterManager<Picture>(oMapFragment.getContext(), oMap);
        oPictureClusterRenderer = new PictureClusterRenderer(oMapFragment.getContext(), oMap, oClusterManager);
        oClusterManager.setRenderer(oPictureClusterRenderer);
        oMap.setOnCameraIdleListener(oClusterManager);
        oMap.setOnMarkerClickListener(oClusterManager);
        oClusterManager.getMarkerCollection().clear();
        oClusterManager.clearItems();
    }
}
