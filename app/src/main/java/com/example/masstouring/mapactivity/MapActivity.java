package com.example.masstouring.mapactivity;

import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masstouring.R;
import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.example.masstouring.recordservice.IRecordServiceCallback;
import com.example.masstouring.recordservice.RecordReceiver;
import com.example.masstouring.recordservice.RecordService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapActivity extends AppCompatActivity implements IItemClickCallback, IRecordServiceCallback, DeleteConfirmationDialog.IDeleteConfirmationDialogCallback {

    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private RecyclerView oRecordsView;
    private RecordsViewAdapter oRecordsViewAdapter;
    private Toolbar oToolbar;
    private RecordReceiver oRecordReceiver;
    private boolean oIsRecordsViewVisible = false;
    private boolean oIsTracePosition = true;
    private Polyline oLastPolyline = null;
    private PolylineOptions oPolylineOptions = new PolylineOptions();
    private OnBackPressedCallback oOnBackPressedCallback;
    private final LinearLayoutManager oManager = new LinearLayoutManager(MapActivity.this);
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, Const.DB_NAME);
    private BoundMapFragment oBoundMapFragment;
    private MapActivtySharedViewModel oMapActivitySharedViewModel;

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
        oToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(oToolbar);
        oToolbar.setVisibility(View.GONE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        oBoundMapFragment = new BoundMapFragment(this, mapFragment);
        oMapActivitySharedViewModel = new ViewModelProvider(this).get(MapActivtySharedViewModel.class);

        oOnBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                oMemoryButton.performClick();
                Log.d(LoggerTag.SYSTEM_PROCESS,"handleOnBackPressed");
            }
        };
        getOnBackPressedDispatcher().addCallback(oOnBackPressedCallback);

        initializeReceiver();
        setButtonClickListeners();
        startRecordService();
        requestCurrentState();

        if(savedInstanceState != null){
            Log.d(LoggerTag.SYSTEM_PROCESS,"onRestoreSavedInstanceState");
        }

        subscribe();
    }

    private void subscribe(){
        oMapActivitySharedViewModel.getRecordState().observe(this, new Observer<RecordState>() {
            @Override
            public void onChanged(RecordState recordState) {
                oStartRecordingButton.setText(recordState.getButtonStringId());
                Toast.makeText(MapActivity.this, getText(recordState.getToastId()), Toast.LENGTH_SHORT).show();
            }
        });
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
        Log.d(LoggerTag.SYSTEM_PROCESS,"onSaveInstanceState MapActivity");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(LoggerTag.SYSTEM_PROCESS,"onRestoreInstanceState MapActivity");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_delete:
                DeleteConfirmationDialog dialog = new DeleteConfirmationDialog();
                dialog.setCallback(this);
                dialog.show(getSupportFragmentManager(), "deleteConfirmationDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    private void setButtonClickListeners(){
        oStartRecordingButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        RecordState state = oMapActivitySharedViewModel.getRecordState().getValue();
                        switch (state){
                            case RECORDING:
                                oMapActivitySharedViewModel.getRecordState().setValue(RecordState.STOP);
                                sendInfoToRecordService(Const.STOP_RECORDING);
                                break;
                            case STOP:
                                oMapActivitySharedViewModel.getRecordState().setValue(RecordState.RECORDING);
                                startRecordService();
                                sendInfoToRecordService(Const.START_RECORDING);
                                break;
                            default:
                                Log.e(LoggerTag.SYSTEM_PROCESS, "unexpected record state detected:" + state.getId());
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
                    oToolbar.setVisibility(View.GONE);
                    oOnBackPressedCallback.setEnabled(false);
                }else{
                    oIsRecordsViewVisible = true;
                    List<RecordItem> data = loadRecords();
                    oRecordsViewAdapter = new RecordsViewAdapter(data, MapActivity.this, MapActivity.this.getApplicationContext());
                    oRecordsView.setAdapter(oRecordsViewAdapter);
                    oRecordsView.setVisibility(View.VISIBLE);
                    oOnBackPressedCallback.setEnabled(true);
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

        oBoundMapFragment.getMap().addPolyline(polylineOptions);
        oBoundMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(fitArea, 0));
        oIsTracePosition = false;
    }

    @Override
    public void onRecordItemLongClick() {
        if(oRecordsViewAdapter.getSelectedItemIdList().size() > 0){
            oToolbar.setVisibility(View.VISIBLE);
        }else{
            oToolbar.setVisibility(View.GONE);
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

    @Override
    public void onReceiveLocationUpdate(Location aLocation, boolean aNeedUpdate) {
        if(oIsTracePosition) {
            oBoundMapFragment.getMap().animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()), 16f));
        }

        if(oMapActivitySharedViewModel.getRecordState().getValue().equals(RecordState.RECORDING) && aNeedUpdate) {
            if(oLastPolyline != null){
                oLastPolyline.remove();
            }
            oPolylineOptions.add(new LatLng(aLocation.getLatitude(), aLocation.getLongitude()));
            oLastPolyline = oBoundMapFragment.getMap().addPolyline(oPolylineOptions);
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "MapActivity Received Location Updates");

    }

    @Override
    public void onReceiveReplyCurrentState(RecordState aRecordState, int aId) {
        oMapActivitySharedViewModel.getRecordState().setValue(aRecordState);
        if(aRecordState == RecordState.RECORDING){
            oPolylineOptions = oDatabaseHelper.restorePolylineOptionsFrom(aId);
            oLastPolyline = oBoundMapFragment.getMap().addPolyline(oPolylineOptions);
            oDatabaseHelper.getLastLatLngFrom(aId).ifPresent(e -> oBoundMapFragment.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(e, 16f)));
        }
        Log.d(LoggerTag.BROADCAST_PROCESS, "MapActivity Received Current State Reply:" + aRecordState);
    }

    @Override
    public void onPositiveClick() {
        List<Integer> list = oRecordsViewAdapter.getSelectedItemIdList();
        for(int id : list){
            oDatabaseHelper.deleteRecord(id);
        }
        oRecordsViewAdapter.setData(oDatabaseHelper.getRecords());
        oRecordsViewAdapter.notifyDataSetChanged();
        oToolbar.setVisibility(View.GONE);
    }

    @Override
    public void onNegativeClick() {

    }
}