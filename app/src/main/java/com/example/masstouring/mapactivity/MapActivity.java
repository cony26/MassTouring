package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.recordservice.ILocationUpdateCallback;
import com.example.masstouring.R;
import com.example.masstouring.recordservice.RecordReceiver;
import com.example.masstouring.recordservice.RecordService;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, IItemClickCallback, ILocationUpdateCallback {

    private GoogleMap mMap;
    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private RecyclerView oRecordsView;
    private RecordState oRecordState = RecordState.STOP;
    private RecordReceiver oRecordReceiver;
    private boolean oIsRecordsViewVisible = false;
    private boolean oIsTracePosition = true;
    private RecordObject oRecordObject;
    private final LinearLayoutManager oManager = new LinearLayoutManager(MapActivity.this);
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onCreate MapActivity");
        setContentView(R.layout.activity_maps);
        oStartRecordingButton = findViewById(R.id.btnStartRecording);
        oMemoryButton = findViewById(R.id.btnMemory);

        oRecordsView = findViewById(R.id.recordsView);
        oManager.setOrientation(LinearLayoutManager.VERTICAL);
        oRecordsView.setLayoutManager(oManager);
        oRecordsView.setVisibility(View.GONE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initializeReceiver();
        setButtonClickListeners();
        startRecordService();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                oIsTracePosition = true;
                return false;
            }
        });
        mMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                if(i == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE){
                    oIsTracePosition = false;
                }
            }
        });
    }


    private void setButtonClickListeners(){
        oStartRecordingButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(oRecordState == RecordState.RECORDING){
                            oRecordState = RecordState.STOP;
                            String endDate = LocalDateTime.now().format(Const.DATE_FORMAT);
                            oRecordObject.setEndDate(endDate);
                            oDatabaseHelper.recordEndInfo(oRecordObject);
                            oStartRecordingButton.setText(R.string.startRecording);
                            Toast.makeText(MapActivity.this, getText(R.string.touringFinishToast), Toast.LENGTH_SHORT).show();
                        }else if(oRecordState == RecordState.STOP){
                            oRecordState = RecordState.RECORDING;
                            oRecordObject = new RecordObject(oDatabaseHelper);
                            oDatabaseHelper.recordStartInfo(oRecordObject);
                            oStartRecordingButton.setText(R.string.stopRecording);
                            Toast.makeText(MapActivity.this, getText(R.string.touringStartToast), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        oMemoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(oIsRecordsViewVisible) {
                    oIsRecordsViewVisible = false;
                    oRecordsView.setVisibility(View.GONE);
                }else{
                    oIsRecordsViewVisible = true;
                    List<RecordItem> data = loadRecords();
                    RecyclerView.Adapter adapter = new RecordsViewAdapter(data, MapActivity.this);
                    oRecordsView.setAdapter(adapter);
                    oRecordsView.setVisibility(View.VISIBLE);
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

    private void startRecordService(){
        Intent i = new Intent(MapActivity.this, RecordService.class);
        startForegroundService(i);
    }

    private void initializeReceiver(){
        oRecordReceiver = new RecordReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.RECORD_SERVICE_ACTION_ID);
        registerReceiver(oRecordReceiver, filter);
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onPause MapActivity");
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onResume MapActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(oRecordReceiver);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onDestroy MapActivity");
    }

    @Override
    public void onRecordItemClick(Map<Integer, LatLng> aMap) {
        if(aMap.size() <= 1)
            return;

        PolylineOptions polylineOptions = createPolylineFrom(aMap);
        LatLngBounds fitArea = createFitAreaFrom(aMap);

        mMap.addPolyline(polylineOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(fitArea, 0));
        oIsTracePosition = false;
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

    @Override
    public void onReceiveLocationUpdate(Location aLocation) {
        if(oIsTracePosition) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()), 16f));
        }

        if(oRecordState.equals(RecordState.RECORDING)) {
            if (oRecordObject.isDifferenceEnough(aLocation)) {
                oRecordObject.addLocation(aLocation);
                oRecordObject.inclementRecordNumber();
                oRecordObject.drawPolyline(mMap);
                oDatabaseHelper.recordPositions(oRecordObject);
            }
        }
    }
}