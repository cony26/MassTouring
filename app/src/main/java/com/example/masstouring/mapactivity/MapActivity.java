package com.example.masstouring.mapactivity;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.recordservice.IRecordServiceCallback;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, IItemClickCallback, IRecordServiceCallback {

    private GoogleMap mMap;
    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private RecyclerView oRecordsView;
    private RecordState oRecordState = RecordState.STOP;
    private RecordReceiver oRecordReceiver;
    private boolean oIsRecordsViewVisible = false;
    private boolean oIsTracePosition = true;
    private Polyline oLastPolyline = null;
    private PolylineOptions oPolylineOptions = new PolylineOptions();
    private final LinearLayoutManager oManager = new LinearLayoutManager(MapActivity.this);
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete:
                Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initializeReceiver();
        setButtonClickListeners();
        startRecordService();
        requestCurrentState();

        if(savedInstanceState != null){
            Log.d(LoggerTag.SYSTEM_PROCESS,"onRestoreSavedInstanceState");
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onResume MapActivity");
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onPause MapActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(oRecordReceiver);
        Log.d(LoggerTag.SYSTEM_PROCESS,"onDestroy MapActivity");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onSaveInstanceState");
        super.onSaveInstanceState(outState);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void setButtonClickListeners(){
        oStartRecordingButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(oRecordState == RecordState.RECORDING){
                            oRecordState = RecordState.STOP;
                            oStartRecordingButton.setText(R.string.startRecording);
                            sendInfoToRecordService(Const.STOP_RECORDING);
                            Toast.makeText(MapActivity.this, getText(R.string.touringFinishToast), Toast.LENGTH_SHORT).show();
                        }else if(oRecordState == RecordState.STOP){
                            oRecordState = RecordState.RECORDING;
                            oStartRecordingButton.setText(R.string.stopRecording);
                            sendInfoToRecordService(Const.START_RECORDING);
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

    private void sendInfoToRecordService(String aStartStopInfo){
        Intent i = new Intent(Const.START_STOP_ACTION_ID);
        i.putExtra(Const.START_STOP_RECORDING_KEY, aStartStopInfo);
        sendBroadcast(i);
        Log.d(LoggerTag.BROADCAST_PROCESS, "MapActivity Sent " + aStartStopInfo);
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
        filter.addAction(Const.LOCATION_UPDATE_ACTION_ID);
        filter.addAction(Const.REPLY_CURRENT_STATE_ACTION_ID);
        registerReceiver(oRecordReceiver, filter);
    }

    private void requestCurrentState(){
        Intent i = new Intent(Const.REQUEST_CURRENT_STATE_ACTION_ID);
        sendBroadcast(i);
        Log.d(LoggerTag.BROADCAST_PROCESS, "MapActivity Sent Current State Request");
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
    public void onReceiveLocationUpdate(Location aLocation, boolean aNeedUpdate) {
        if(oIsTracePosition) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()), 16f));
        }

        if(oRecordState.equals(RecordState.RECORDING) && aNeedUpdate) {
            if(oLastPolyline != null){
                oLastPolyline.remove();
            }
            oPolylineOptions.add(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()));
            oLastPolyline = mMap.addPolyline(oPolylineOptions);
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "MapActivity Received Location Updates");

    }

    @Override
    public void onReceiveReplyCurrentState(RecordState aRecordState, int aId) {
        oRecordState = aRecordState;
        if(oRecordState == RecordState.RECORDING){
            oStartRecordingButton.setText(R.string.stopRecording);
            oPolylineOptions = oDatabaseHelper.restorePolylineOptionsFrom(aId);
            oLastPolyline = mMap.addPolyline(oPolylineOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(oDatabaseHelper.getLastLatLngFrom(aId), 16f));
        }else if(oRecordState == RecordState.STOP){
            oStartRecordingButton.setText(R.string.startRecording);
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "MapActivity Received Current State Reply:" + aRecordState);
    }
}