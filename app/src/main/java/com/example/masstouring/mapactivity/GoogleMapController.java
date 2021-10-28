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
import com.example.masstouring.event.PolylineRenderEvent;
import com.example.masstouring.event.RemoveRecordItemEvent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleMapController implements OnMapReadyCallback, LifecycleObserver, ILocationUpdateCallback {
    private GoogleMap oMap;
    private ClusterManager<Picture> oClusterManager;
    private PictureClusterRenderer oPictureClusterRenderer = null;
    private ClusterDistributer oClusterDistributer;
    private final SupportMapFragment oMapFragment;
    private final MapActivtySharedViewModel oMapActivityViewModel;
    private final GoogleMapViewModel oGoogleMapViewModel;
    private Map<Integer, List<Polyline>> oRenderedPolylineMap = new HashMap<>();
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
        new LifeCycleLogger(oMapFragment.getViewLifecycleOwner(), oMapFragment.getClass().getSimpleName());
        oMapActivityViewModel = new ViewModelProvider(aAppCompatActivity).get(MapActivtySharedViewModel.class);
        oGoogleMapViewModel = new ViewModelProvider(oMapFragment.getParentFragment()).get(GoogleMapViewModel.class);
        subscribeLiveData();
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

        oMapActivityViewModel.getPolylineRenderEvent().observe(oMapFragment, new Observer<PolylineRenderEvent>() {
            @Override
            public void onChanged(PolylineRenderEvent polylineRenderEvent) {
                RecordItem item = polylineRenderEvent.getContentIfNotHandled();
                if(item != null){
                    List<PolylineOptions> polylineOptionsList = item.createPolylineOptions();
                    List<Polyline> polylineList = new ArrayList<>();
                    for(PolylineOptions polylineOptions : polylineOptionsList)
                        polylineList.add(oMap.addPolyline(polylineOptions));

                    oRenderedPolylineMap.put(item.getId(), polylineList);
                    oMapActivityViewModel.getRenderedIdList().add(item.getId());
                    addPictureMarkersOnMapAsyncly(item);
                }
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

        oMapActivityViewModel.getRemoveRecordItemEvent().observe(oMapFragment, new Observer<RemoveRecordItemEvent>() {
            @Override
            public void onChanged(RemoveRecordItemEvent removeRecordItemEvent) {
                RecordItem item = removeRecordItemEvent.getContentIfNotHandled();
                if(item != null){
                    removePolyline(item.getId());
                    removePictureMarkersOnMapAsyncly(item);
                    oMapActivityViewModel.getRenderedIdList().remove((Object)item.getId());
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
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<Picture> picturesList = MediaAccessUtil.loadPictures(oMapFragment.getContext(), aRecordItem);
                drawMarkers(picturesList);
            }
        });
    }

    public void removePictureMarkersOnMapAsyncly(RecordItem aRecordItem){
        MapActivity.cExecutors.execute(new Runnable() {
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

    public void removePolyline(int aId){
        oRenderedPolylineMap.get(aId).stream().forEach(polyline -> polyline.remove());
        oRenderedPolylineMap.remove(aId);
    }

    public void moveCameraToLastLocation(int aRecordId){
        LatLng latLng = oGoogleMapViewModel.getLastLatLngFrom(aRecordId);
        if(latLng != null){
            oMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f));
        }
    }

    public void initialize(){
        oClusterDistributer.detachDistributedView();
        oClusterManager.clearItems();
        oClusterManager.cluster();
        oMapActivityViewModel.getRenderedIdList().clear();
        oRenderedPolylineMap.clear();
        oMap.clear();
        oGoogleMapViewModel.setRecordingPolylineOptions(new PolylineOptions());
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
        oMapActivityViewModel.getLocationUpdateCallback().setValue(null);
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
