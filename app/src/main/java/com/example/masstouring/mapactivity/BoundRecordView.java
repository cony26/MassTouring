package com.example.masstouring.mapactivity;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.time.LocalDateTime;
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
    private List<Polyline> oPrevPolylineList = new ArrayList<>();
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

        List<PolylineOptions> polylineOptionsList = createPolylineFrom(locationMap, aRecordItem.getSpeedkmphMap());
        LatLngBounds fitArea = createFitAreaFrom(locationMap);

        oPrevPolylineList.stream().forEach(polyline -> polyline.remove());
        oPrevPolylineList.clear();

        for(PolylineOptions polylineOptions : polylineOptionsList) {
            oPrevPolylineList.add(oMapFragment.getMap().addPolyline(polylineOptions));
        }
        oMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(fitArea, 0));
        oMapActivitySharedViewModel.getIsTracePosition().setValue(false);

        addPictureMarkersOnMapAsyncly(aRecordItem);
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

    private void addPictureMarkersOnMapAsyncly(RecordItem aRecordItem){
        long startDateSecond = aRecordItem.getStartDate().toEpochSecond(Const.STORED_OFFSET);

        long endDateSecond;
        Map<Integer, String> timeStampMap = aRecordItem.getTimeStampMap();
        if(aRecordItem.getEndDate() == null){
            endDateSecond = LocalDateTime.parse(timeStampMap.get(timeStampMap.size() - 1), Const.DATE_FORMAT).toEpochSecond(Const.STORED_OFFSET);
        } else {
            endDateSecond = aRecordItem.getEndDate().toEpochSecond(Const.STORED_OFFSET);
        }

        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                List<Picture> picturesList = loadPictures(aRecordItem, startDateSecond, endDateSecond);
                oMapFragment.drawMarkers(picturesList);
            }
        });
    }

    private List<Picture> loadPictures(RecordItem aRecordItem, long startDate, long endDate){
        Log.d(LoggerTag.RECORD_RECYCLER_VIEW, "startDate:" + startDate + ", endDate:" + endDate);

        List<Picture> pictureList = new ArrayList<>();
        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_ADDED
        };
        String selection = MediaStore.Images.Media.DATE_ADDED + " >= " + startDate + " AND " + MediaStore.Images.Media.DATE_ADDED + " <= " + endDate;
        String[] selectionArgs = new String[]{};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " ASC";

        try(Cursor cursor = oRecordsView.getContext().getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )){
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

            while(cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                int date = cursor.getInt(dateAddedColumn);

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                LatLng nearestLatLng = fetchNearestLatLng(aRecordItem.getTimeStampMap(), aRecordItem.getLocationMap(), date);
                pictureList.add(new Picture(contentUri, date, nearestLatLng));
            }
        }

        for(Picture picture : pictureList){
            Log.d(LoggerTag.RECORD_RECYCLER_VIEW, picture.toString());
        }

        return pictureList;
    }

    private LatLng fetchNearestLatLng(Map<Integer, String> aTimeStampMap, Map<Integer, LatLng> aLocationMap, int aFetchTimeStamp){
        long candidateTimeStamp;
        int size = aTimeStampMap.size();
        int fetchIndex = size - 1;
        for(int i = 0; i < size; i++){
            candidateTimeStamp = LocalDateTime.parse(aTimeStampMap.get(i), Const.DATE_FORMAT).toEpochSecond(Const.STORED_OFFSET);
            if(candidateTimeStamp >= aFetchTimeStamp){
                fetchIndex = i;
                break;
            }
        }

        return aLocationMap.get(fetchIndex);
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
