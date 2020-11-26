package com.example.masstouring;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, IItemClickCallback, ILocationUpdateCallback{

    private GoogleMap mMap;
    private int oRecordNumber = 0;
    private int oId;
    private final List<Location> oLocations= new ArrayList<>();
    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private RecyclerView oRecordsView;
    private final LinearLayoutManager oManager = new LinearLayoutManager(MapActivity.this);
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);
    private boolean oIsRecordsViewVisible = false;
    private boolean oIsStartTouring = false;
    private Location oLastMappedLocation = null;
    private RecordReceiver oRecordReceiver;
    private boolean oIsTracePosition = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LoggerTag.PROCESS,"onCreate MapActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        oStartRecordingButton = findViewById(R.id.btnStartRecording);
        oMemoryButton = findViewById(R.id.btnMemory);
        oRecordsView = findViewById(R.id.recordsView);
        oManager.setOrientation(LinearLayoutManager.VERTICAL);
        oRecordsView.setLayoutManager(oManager);
        oRecordReceiver = new RecordReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.RECORD_SERVICE_ACTION_ID);
        registerReceiver(oRecordReceiver, filter);

        setButtonClickListeners();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        startWatchLocation();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

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
                        if(oIsStartTouring){
                            try(SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()){
                                LocalDateTime now = LocalDateTime.now();
                                oDatabaseHelper.putRecordsEndInfo(db, oId, now.format(Const.DATE_FORMAT), oRecordNumber);
                            }
                            oIsStartTouring = false;
                            oRecordNumber = 0;
                            oStartRecordingButton.setText(R.string.startRecording);
                            Toast.makeText(MapActivity.this, getText(R.string.touringFinishToast), Toast.LENGTH_SHORT).show();
                        }else{
                            oIsStartTouring = true;
                            oLocations.clear();
                            oId = oDatabaseHelper.getUniqueID();
                            try(SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()){
                                LocalDateTime now = LocalDateTime.now();
                                oDatabaseHelper.putRecordsStartInfo(db, oId, now.format(Const.DATE_FORMAT));
                            }
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
                    List<RecordsItem> data = loadRecords();
                    RecyclerView.Adapter adapter = new RecordsViewAdapter(data, MapActivity.this);
                    oRecordsView.setAdapter(adapter);
                    oRecordsView.setVisibility(View.VISIBLE);
                }
            }
        });

    }

    private List<RecordsItem> loadRecords(){
        List<RecordsItem> data;
        try(SQLiteDatabase db = oDatabaseHelper.getReadableDatabase()){
            data = oDatabaseHelper.getRecords(db);
        }
        for(RecordsItem item : data){
            Log.d(LoggerTag.RECORDS, item.toString());
        }
        return data;
    }

    private void startWatchLocation(){
        Intent i = new Intent(MapActivity.this, RecordService.class);
        startForegroundService(i);
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(LoggerTag.PROCESS,"onPause MapActivity");
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LoggerTag.PROCESS,"onResume MapActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(oRecordReceiver);
        Log.d(LoggerTag.PROCESS,"onDestroy MapActivity");
    }

    private boolean isDifferenceEnough(Location aLocation){
        if(oLastMappedLocation == null){
            oLastMappedLocation = aLocation;
            return true;
        }

        float distance = aLocation.distanceTo(oLastMappedLocation);
        Log.d(LoggerTag.LOCATION, "(latitude, longitude) = (" + aLocation.getLatitude() + "," + aLocation.getLongitude() + ")");
        Log.d(LoggerTag.LOCATION, "(distance, limit) = (" + distance + "," + Const.DISTANCE_GAP + ")");
        if(distance >= Const.DISTANCE_GAP){
            oLastMappedLocation = aLocation;
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void onRecordItemClick(Map<Integer, LatLng> aMap) {
        if(aMap.size() <= 1)
            return;

        PolylineOptions polylineOptions = new PolylineOptions();
        double minLat;
        double maxLat;
        double minLon;
        double maxLon;
        Set<Double> latSet = new HashSet<>();
        Set<Double> lonSet = new HashSet<>();

        for(int i = 0; i < aMap.size(); i++){
            LatLng latLng = aMap.get(i);
            polylineOptions.add(latLng);
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

        mMap.addPolyline(polylineOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(area, 0));

        StringBuilder builder = new StringBuilder();
        builder.append("[lat1, lon1] = [").append(minLat).append(",").append(minLon).append("]")
                .append("[lat2, lon2] = [").append(maxLat).append(",").append(maxLon).append("]");
        Log.d(LoggerTag.LOCATION, builder.toString());
        oIsTracePosition = false;
    }

    @Override
    public void onReceiveLocationUpdate(Location aLocation) {
        if(oIsTracePosition) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()), 16f));
        }

        if(oIsStartTouring) {
            if (isDifferenceEnough(aLocation)) {
                try (SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()) {
                    oDatabaseHelper.putPositions(db, oId, oRecordNumber, aLocation.getLatitude(), aLocation.getLongitude(), LocalDateTime.now().format(Const.DATE_FORMAT));
                }
                oLocations.add(oLastMappedLocation);
                oRecordNumber++;
            }
        }

        if(oRecordNumber > 0){
            PolylineOptions polylineOptions = new PolylineOptions();
            for(Location mappedLocation : oLocations){
                polylineOptions.add(new LatLng(mappedLocation.getLatitude(), mappedLocation.getLongitude()));
            }
            mMap.addPolyline(polylineOptions);
        }
    }
}