package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ViewModelProvider;

import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.common.MediaAccessUtil;
import com.example.masstouring.event.FitAreaEvent;
import com.example.masstouring.event.RestoreFromServiceEvent;
import com.example.masstouring.recordservice.ILocationUpdateCallback;
import com.example.masstouring.viewmodel.GoogleMapViewModel;
import com.example.masstouring.viewmodel.MapActivtySharedViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;

import java.util.*;
import java.util.stream.Collectors;

public class GoogleMapController implements OnMapReadyCallback, LifecycleObserver, ILocationUpdateCallback {
    private GoogleMap oMap;
    private ClusterManager<Picture> oClusterManager;
    private PictureClusterRenderer oPictureClusterRenderer = null;
    private ClusterDistributer oClusterDistributer;
    private Map<Integer, PolylineInfo> lastPolylineInfoMap = null;
    private final SupportMapFragment oMapFragment;
    private final MapActivtySharedViewModel oMapActivityViewModel;
    private final GoogleMapViewModel oGoogleMapViewModel;
    private final PrioritizedOnBackPressedCallback oOnBackPressedCallbackWhenClusterDistributed = new PrioritizedOnBackPressedCallback(false, PrioritizedOnBackPressedCallback.CLUSTER_DISTRIBUTED) {
        @Override
        public void handleOnBackPressed() {
            if(oGoogleMapViewModel.isClusterDistributed().getValue()){
                oClusterDistributer.detachDistributedView();
            }
            Log.d(LoggerTag.SYSTEM_PROCESS,"back pressed when cluster distributed");
        }
    };

    public GoogleMapController(AppCompatActivity aAppCompatActivity, SupportMapFragment aMapFragment){
        oMapFragment = aMapFragment;
        oMapFragment.getMapAsync(this);
        oMapFragment.getLifecycle().addObserver(this);
        new LifeCycleLogger(oMapFragment.getViewLifecycleOwner());
        oMapActivityViewModel = new ViewModelProvider(aAppCompatActivity).get(MapActivtySharedViewModel.class);
        oGoogleMapViewModel = new ViewModelProvider(oMapFragment.getParentFragment()).get(GoogleMapViewModel.class);
        BackPressedCallbackRegisterer.getInstance().register(oOnBackPressedCallbackWhenClusterDistributed);
        oMapActivityViewModel.getLocationUpdateCallback().setValue(this);
    }

    private void subscribeLiveData(){
        oGoogleMapViewModel.isClusterDistributed().observe(oMapFragment, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aDistributed) {
                if(aDistributed){
                    oOnBackPressedCallbackWhenClusterDistributed.setEnabled(true);
                }else{
                    oOnBackPressedCallbackWhenClusterDistributed.setEnabled(false);
                }
            }
        });

        oMapActivityViewModel.getRestoreEvent().observe(oMapFragment, new Observer<RestoreFromServiceEvent>() {
            @Override
            public void onChanged(RestoreFromServiceEvent aEvent) {
                Integer id = aEvent.getContentIfNotHandled();
                if(id != null){
                    oGoogleMapViewModel.restorePolylineOptionsFrom(oMap, id);
                    moveCameraToLastLocation(id);
                }
            }
        });

        oGoogleMapViewModel.getRenderedPolylineInfo().observe(oMapFragment, new Observer<Map<Integer, PolylineInfo>>() {
            @Override
            public void onChanged(Map<Integer, PolylineInfo> polylineInfoMap) {
                //add Polyline
                for(int id : polylineInfoMap.keySet()){
                    PolylineInfo info = polylineInfoMap.get(id);
                    if(info == null){
                        Log.e(LoggerTag.POLYLINE_PROCESS, "polylineInfo " + id + " is null.");
                        continue;
                    }

                    List<Polyline> polylineList = info.getPolylineList();
                    if(polylineList != null && polylineList.stream().filter(Objects::nonNull).anyMatch(Polyline::isVisible)){
                        Log.d(LoggerTag.POLYLINE_PROCESS, "polyline " + id + " has been already drawn");
                        continue;
                    }

                    //draw new Polyline
                    List<PolylineOptions> polylineOptionsList = info.getPolylineOptionsList();
                    List<Polyline> renderedPolylineList = new ArrayList<>();
                    for(PolylineOptions polylineOptions : polylineOptionsList){
                        renderedPolylineList.add(oMap.addPolyline(polylineOptions));
                    }
                    info.setPolylineList(renderedPolylineList);
                    addPictureMarkersOnMapAsyncly(oMapActivityViewModel.getRecord(id));
                }

                //remove Polyline
                if(lastPolylineInfoMap != null){
                    List<Integer> shouldRemoveIdList = lastPolylineInfoMap.keySet().stream().filter(id -> !polylineInfoMap.containsKey(id)).collect(Collectors.toList());
                    for(int id : shouldRemoveIdList){
                        PolylineInfo polylineInfo = lastPolylineInfoMap.get(id);
                        if(polylineInfo != null){
                            List<Polyline> polylineList = polylineInfo.getPolylineList();
                            if(polylineList != null){
                                polylineList.stream().filter(Objects::nonNull).forEach(Polyline::remove);
                                Log.v(LoggerTag.POLYLINE_PROCESS, "polyline " + id + " was removed");
                            }
                        }
                        removePictureMarkersOnMapAsyncly(oMapActivityViewModel.getRecord(id));
                    }
                }

                lastPolylineInfoMap = new HashMap<>(polylineInfoMap);
            }
        });

        oMapActivityViewModel.getFitAreaEvent().observe(oMapFragment, new Observer<FitAreaEvent>() {
            @Override
            public void onChanged(FitAreaEvent fitAreaEvent) {
                if(oMapActivityViewModel.isNothingRendered()){
                    RecordItem item = fitAreaEvent.getContentIfNotHandled();
                    if(item != null){
                        LatLngBounds fitArea = item.createFitAreaFrom();
                        oMap.moveCamera(CameraUpdateFactory.newLatLngBounds(fitArea, 0));
                    }
                }
            }
        });
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
                oMapActivityViewModel.isTracePosition().setValue(true);
                return false;
            }
        });
        oMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if(i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                    oMapActivityViewModel.isTracePosition().setValue(false);
                }
            }
        });
        oMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                if(oGoogleMapViewModel.isClusterDistributed().getValue()){
                    oClusterDistributer.updateClusterScreenPosition(oMap);
                }
            }
        });
        instantiateClusterManagers();
        subscribeLiveData();
    }

    @Override
    public void onReceiveLocationUpdate(Location aLocation, boolean aNeedUpdate) {
        if(oMapActivityViewModel.isTracePosition().getValue()) {
            oMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()), 16f));
        }

        if(!aNeedUpdate)
            return;

        if(oMapActivityViewModel.isRecording()) {
            oGoogleMapViewModel.updatePolyline(oMap, aLocation);
        }

        Log.d(LoggerTag.SYSTEM_PROCESS, "Location Updates");
    }


    public void addPictureMarkersOnMapAsyncly(RecordItem aRecordItem){
        TouringMapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<Picture> picturesList = MediaAccessUtil.loadPictures(oMapFragment.getContext(), aRecordItem);
                drawMarkers(picturesList);
            }
        });
    }

    public void removePictureMarkersOnMapAsyncly(RecordItem aRecordItem){
        TouringMapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<Picture> picturesList = MediaAccessUtil.loadPictures(oMapFragment.getContext(), aRecordItem);
                removeMarkers(picturesList);
            }
        });
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

    public void moveCameraToLastLocation(int aRecordId){
        LatLng latLng = oGoogleMapViewModel.getLastLatLngFrom(aRecordId);
        if(latLng != null){
            oMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        }
    }

    public void initialize(){
        clearMapContents();
        oGoogleMapViewModel.setRecordingPolylineOptions(new PolylineOptions());
    }

    private void clearMapContents(){
        oClusterDistributer.detachDistributedView();
        oClusterManager.clearItems();
        oClusterManager.cluster();
        lastPolylineInfoMap = null;
        oMap.clear();
        oGoogleMapViewModel.clearPolyline();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume(){
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void clearAll(){
        oMapActivityViewModel.getLocationUpdateCallback().setValue(null);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy(){
        clearMapContents();
    }

    private void instantiateClusterManagers(){
        Context context = oMapFragment.getContext();
        oClusterManager = new ClusterManager<Picture>(context, oMap);
        oPictureClusterRenderer = new PictureClusterRenderer(context, oMap, oClusterManager);
        oClusterDistributer = new ClusterDistributer(context, oGoogleMapViewModel);
        oPictureClusterRenderer.setClusterUpdatedListener(oClusterDistributer);
        oClusterManager.setRenderer(oPictureClusterRenderer);
        oMap.setOnCameraIdleListener(oClusterManager);
        oMap.setOnMarkerClickListener(oClusterManager);
        oClusterManager.setOnClusterClickListener(new ClusterManager.OnClusterClickListener<Picture>() {
            @Override
            public boolean onClusterClick(Cluster<Picture> cluster) {
                oClusterDistributer.attachDistributedView(oMapFragment.getActivity());
                oClusterDistributer.onClusterClick(cluster);
                return false;
            }
        });
    }
}
