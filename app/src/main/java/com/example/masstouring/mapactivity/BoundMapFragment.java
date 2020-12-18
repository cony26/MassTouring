package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

public class BoundMapFragment implements OnMapReadyCallback, LifecycleObserver {
    private GoogleMap mMap;
    private SupportMapFragment oMapFragment;
    private MapActivtySharedViewModel aMapActivityViewModel;

    public BoundMapFragment(LifecycleOwner aLifeCycleOwner, SupportMapFragment aMapFragment){
        aMapFragment.getMapAsync(this);
        oMapFragment = aMapFragment;
        aMapActivityViewModel = new ViewModelProvider(aMapFragment.getActivity()).get(MapActivtySharedViewModel.class);
        aLifeCycleOwner.getLifecycle().addObserver(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(ActivityCompat.checkSelfPermission(oMapFragment.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(oMapFragment.getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                aMapActivityViewModel.getIsTracePosition().setValue(true);
                return false;
            }
        });
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if(i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                    aMapActivityViewModel.getIsTracePosition().setValue(false);
                }
            }
        });
    }

    public GoogleMap getMap() {
        return mMap;
    }
}
