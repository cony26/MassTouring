package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoundRecordView implements LifecycleObserver, IItemClickCallback{
    private RecyclerView oRecordsView;
    private final LinearLayoutManager oManager;
    private RecordsViewAdapter oRecordsViewAdapter;
    private MapActivtySharedViewModel oMapActivitySharedViewModel;
    private final DatabaseHelper oDatabaseHelper;
    private BoundMapFragment oMapFragment;
    private static final int alpha = 0x80000000;

    public BoundRecordView(LifecycleOwner aLifeCycleOwner, RecyclerView aRecordView, MapActivtySharedViewModel aViewModel, Context aContext){
        aLifeCycleOwner.getLifecycle().addObserver(this);
        oRecordsView = aRecordView;
        oManager = new LinearLayoutManager(aRecordView.getContext());
        oMapActivitySharedViewModel = aViewModel;
        oDatabaseHelper = new DatabaseHelper(aContext, Const.DB_NAME);
        oManager.setOrientation(LinearLayoutManager.VERTICAL);
        oRecordsView.setLayoutManager(oManager);
        oRecordsView.setVisibility(View.GONE);

        subscribe(aLifeCycleOwner, aContext);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void initialize(){

    }

    private void subscribe(LifecycleOwner aLifeCycleOwner, Context aContext){
        oMapActivitySharedViewModel.getIsRecordsViewVisible().observe(aLifeCycleOwner, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isVisible) {
                if(isVisible){
                    List<RecordItem> data = loadRecords();
                    oRecordsViewAdapter = new RecordsViewAdapter(data, BoundRecordView.this, aContext);
                    oRecordsView.setAdapter(oRecordsViewAdapter);
                    oRecordsView.setVisibility(View.VISIBLE);
                }else{
                    oRecordsView.setVisibility(View.GONE);
                }
            }
        });
    }

    private List<RecordItem> loadRecords(){
        RecordItem recordItemArray[] = new RecordItem[oDatabaseHelper.getRecordSize()];
        Arrays.fill(recordItemArray, RecordItem.EMPTY_RECORD);

        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<RecordItem> data = oDatabaseHelper.getRecords();
                data.stream()
                        .filter(recordItem -> oMapFragment.isRendered(recordItem.getId()))
                        .forEach(recordItem -> recordItem.setRendered(true));
                while(oRecordsViewAdapter == null){
                    try{
                        Thread.sleep(500);
                    }catch(InterruptedException e){
                        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "interrupted");
                    }
                }
                oRecordsViewAdapter.setData(data);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        oRecordsViewAdapter.notifyDataSetChanged();
                    }
                });
            }
        });

        return Arrays.asList(recordItemArray);
    }

    @Override
    public void onRecordItemClick(RecordItem aRecordItem) {
        Map locationMap = aRecordItem.getLocationMap();
        if(locationMap.size() <= 1)
            return;

        int id = aRecordItem.getId();
        if(aRecordItem.isRendered()){
            oMapFragment.removePolyline(id);
            oMapFragment.removePictureMarkersOnMapAsyncly(aRecordItem);
        }else{
            LatLngBounds fitArea = createFitAreaFrom(locationMap);
            oMapFragment.fitCameraTo(fitArea, 0);

            List<PolylineOptions> polylineOptionsList = createPolylineFrom(locationMap, aRecordItem.getSpeedkmphMap());
            oMapFragment.drawPolyline(polylineOptionsList, id);
            oMapFragment.addPictureMarkersOnMapAsyncly(aRecordItem);
        }

        oMapActivitySharedViewModel.getIsTracePosition().setValue(false);
    }

    @Override
    public void onRecordItemLongClick() {
        if(oRecordsViewAdapter.getSelectedItemIdList().size() > 0){
            oMapActivitySharedViewModel.getToolbarVisibility().setValue(View.VISIBLE);
        }else{
            oMapActivitySharedViewModel.getToolbarVisibility().setValue(View.GONE);
        }
    }

    private List<PolylineOptions> createPolylineFrom(Map<Integer, LatLng> aLatLngMap, Map<Integer, Double> aSpeedKmphMap){
        double maxSpeedKmph = aSpeedKmphMap.values().stream().max(Double::compareTo).orElse(0.0);
        double minSpeedKmph = aSpeedKmphMap.values().stream().min(Double::compareTo).orElse(0.0);
        double diff = maxSpeedKmph - minSpeedKmph;

        List<PolylineOptions> polylineOptionsList = new ArrayList<>();
        PolylineOptions polylineOptions = null;
        int prevColor = Integer.MIN_VALUE;

        for(int i = 0; i < aLatLngMap.size(); i++){
            int color = calculateColorForNewSpeed(aSpeedKmphMap.get(i), diff, minSpeedKmph);

            if(polylineOptions == null){
                polylineOptions = new PolylineOptions().color(color);
                prevColor = color;
                continue;
            }

            if(color != prevColor){
                polylineOptionsList.add(polylineOptions);
                polylineOptions = new PolylineOptions().color(color);
            }
            polylineOptions.add(aLatLngMap.get(i));
            prevColor = color;
        }
        return polylineOptionsList;
    }

    private int calculateColorForNewSpeed(double aNewSpeedKmph, double aDiffSpeedKmph, double aMinSpeedKmph){
        int color;
        if(aNewSpeedKmph < aMinSpeedKmph + aDiffSpeedKmph / 3){
            color = Color.RED + alpha;
        }else if(aNewSpeedKmph < aMinSpeedKmph + aDiffSpeedKmph * 2 / 3){
            color = Color.GREEN + alpha;
        }else{
            color = Color.BLUE + alpha;
        }

        return color;
    }

    private LatLngBounds createFitAreaFrom(Map<Integer, LatLng> aMap){
        double minLat;
        double maxLat;
        double minLon;
        double maxLon;
        Set<Double> latSet = new HashSet<>();
        Set<Double> lonSet = new HashSet<>();

        for(int i = 0; i < aMap.size(); i++){
            LatLng latLng = aMap.get(i);
            latSet.add(latLng.latitude);
            lonSet.add(latLng.longitude);
        }
        maxLat = latSet.stream().max(Double::compareTo).get();
        minLat = latSet.stream().min(Double::compareTo).get();
        maxLon = lonSet.stream().max(Double::compareTo).get();
        minLon = lonSet.stream().min(Double::compareTo).get();

        LatLngBounds area = new LatLngBounds(
                new LatLng(minLat, minLon),
                new LatLng(maxLat, maxLon)
        );
        StringBuilder builder = new StringBuilder();
        builder.append("FitArea:")
                .append("[lat1, lon1] = [").append(minLat).append(",").append(minLon).append("]")
                .append("[lat2, lon2] = [").append(maxLat).append(",").append(maxLon).append("]");
        Log.d(LoggerTag.LOCATION, builder.toString());

        return area;
    }

    public void deleteSelectedItems(){
        List<Integer> list = oRecordsViewAdapter.getSelectedItemIdList();
        for(int id : list){
            oDatabaseHelper.deleteRecord(id);
        }
        oRecordsViewAdapter.setData(oDatabaseHelper.getRecords());
        oRecordsViewAdapter.notifyDataSetChanged();
    }

    public void setMapFragment(BoundMapFragment aMapFragment) {
        oMapFragment = aMapFragment;
    }
}
