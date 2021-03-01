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
import com.example.masstouring.common.MediaAccessUtil;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.recordservice.ILocationUpdateCallback;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoundMapFragment implements OnMapReadyCallback, LifecycleObserver, ILocationUpdateCallback {
    private GoogleMap oMap;
    private ClusterManager<Picture> oClusterManager;
    private PictureClusterRenderer oPictureClusterRenderer = null;
    private SupportMapFragment oMapFragment;
    private MapActivtySharedViewModel aMapActivityViewModel;
    private Polyline oRecordingLastPolyline = null;
    private PolylineOptions oRecordingPolylineOptions = null;
    private Map<Integer, List<Polyline>> oRenderedPolylineMap = new HashMap<>();
    private DatabaseHelper oDatabaseHelper;
    private List<Integer> oRenderedIdList = new ArrayList<>();

    public BoundMapFragment(LifecycleOwner aLifeCycleOwner, SupportMapFragment aMapFragment){
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
    }

    @Override
    public void onReceiveLocationUpdate(Location aLocation, boolean aNeedUpdate) {
        if(aMapActivityViewModel.getIsTracePosition().getValue()) {
            oMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()), 16f));
        }

        if(!aNeedUpdate)
            return;

        if(aMapActivityViewModel.isRecording()) {
            if(oRecordingLastPolyline != null){
                oRecordingLastPolyline.remove();
            }

            if(oRecordingPolylineOptions != null){
                oRecordingPolylineOptions.add(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()));
                oRecordingLastPolyline = oMap.addPolyline(oRecordingPolylineOptions);
            }
        }
        Log.d(LoggerTag.SYSTEM_PROCESS, "Location Updates");
    }

    public boolean isRendered(int aId){
        return oRenderedIdList.contains(aId);
    }

    public void drawPolyline(List<PolylineOptions> aPolylineOptions, int aId){
        List<Polyline> polylineList = new ArrayList<>();
        for(PolylineOptions polylineOptions : aPolylineOptions)
            polylineList.add(oMap.addPolyline(polylineOptions));

        oRenderedPolylineMap.put(aId, polylineList);
    }

    public void addPictureMarkersOnMapAsyncly(RecordItem aRecordItem){
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<Picture> picturesList = MediaAccessUtil.loadPictures(oMapFragment.getContext(), aRecordItem);
                drawMarkers(picturesList);
            }
        });

        oRenderedIdList.add(aRecordItem.getId());
    }

    public void removePictureMarkersOnMapAsyncly(RecordItem aRecordItem){
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<Picture> picturesList = MediaAccessUtil.loadPictures(oMapFragment.getContext(), aRecordItem);
                removeMarkers(picturesList);
            }
        });

        oRenderedIdList.remove((Object)aRecordItem.getId());
    }

    private void drawMarkers(List<Picture> aPictureList){
        for(Picture picture : aPictureList){
            oClusterManager.addItem(picture);
        }
        callClusterOnUiThread();
    }

    private void removeMarkers(List<Picture> aPictureList){
        for(Picture picture : aPictureList){
            oClusterManager.removeItem(picture);
        }
        callClusterOnUiThread();
    }

    private void callClusterOnUiThread(){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                oClusterManager.cluster();
            }
        });
    }

    public void removePolyline(int aId){
        oRenderedPolylineMap.get(aId).stream().forEach(polyline -> polyline.remove());
        oRenderedPolylineMap.remove(aId);
    }


    public void restorePolyline(int aRecordId){
        if(oRecordingPolylineOptions == null){
            oRecordingPolylineOptions = oDatabaseHelper.restorePolylineOptionsFrom(aRecordId);
            oRecordingLastPolyline = oMap.addPolyline(oRecordingPolylineOptions);
        }
    }

    public void moveCameraToLastLocation(int aRecordId){
        oDatabaseHelper.getLastLatLngFrom(aRecordId).ifPresent(e -> oMap.moveCamera(CameraUpdateFactory.newLatLngZoom(e, 16f)));
    }

    public void fitCameraTo(LatLngBounds aLatLngBounds, int aPadding){
        oMap.moveCamera(CameraUpdateFactory.newLatLngBounds(aLatLngBounds, aPadding));
    }

    public void initialize(){
        oClusterManager.clearItems();
        oMap.clear();
        oRenderedIdList.clear();
        oRenderedPolylineMap.clear();
        oRecordingPolylineOptions = new PolylineOptions();
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(){
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void clearAll(){
//        oClusterManager.clearItems();
//        oClusterManager.cluster();
//        oPictureClusterRenderer.setOnClusterClickListener(null);
//        oPictureClusterRenderer.setOnClusterItemClickListener(null);
//        oClusterManager.getMarkerCollection().clear();
//        oPictureClusterRenderer.onRemove();
    }

    private void instantiateClusterManagers(){
        oClusterManager = new ClusterManager<Picture>(oMapFragment.getContext(), oMap);
        oPictureClusterRenderer = new PictureClusterRenderer(oMapFragment.getContext(), oMap, oClusterManager);
        oClusterManager.setRenderer(oPictureClusterRenderer);
        oMap.setOnCameraIdleListener(oClusterManager);
        oMap.setOnMarkerClickListener(oClusterManager);
    }

}
