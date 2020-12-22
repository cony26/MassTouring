package com.example.masstouring.mapactivity;

import android.content.Context;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

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
        List<RecordItem> data = oDatabaseHelper.getRecords();

        for(RecordItem item : data){
            Log.d(LoggerTag.RECORDS, item.toString());
        }

        return data;
    }

    @Override
    public void onRecordItemClick(Map<Integer, LatLng> aMap) {
        if(aMap.size() <= 1)
            return;

        PolylineOptions polylineOptions = createPolylineFrom(aMap);
        LatLngBounds fitArea = createFitAreaFrom(aMap);

        oMapFragment.getMap().addPolyline(polylineOptions);
        oMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(fitArea, 0));
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

    private PolylineOptions createPolylineFrom(Map<Integer, LatLng> aMap){
        PolylineOptions polylineOptions = new PolylineOptions();
        for(int i = 0; i < aMap.size(); i++){
            LatLng latLng = aMap.get(i);
            polylineOptions.add(latLng);
        }
        return polylineOptions;
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