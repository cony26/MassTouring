package com.example.masstouring;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.masstouring.common.Const;
import com.example.masstouring.common.LoggerTag;
import com.example.masstouring.database.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, IItemClickCallback{

    private FusedLocationProviderClient oFusedClient;
    private SettingsClient oSetClient;
    private LocationSettingsRequest oLocSetReq;
    private LocationCallback oLocCallback;
    private LocationRequest oLocReq;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        oStartRecordingButton = findViewById(R.id.btnStartRecording);
        oMemoryButton = findViewById(R.id.btnMemory);
        oRecordsView = findViewById(R.id.recordsView);
        oManager.setOrientation(LinearLayoutManager.VERTICAL);
        oRecordsView.setLayoutManager(oManager);
        setButtonClickListeners();

        initializeGps();
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
                            Toast.makeText(MapActivity.this, "Touring Finish!", Toast.LENGTH_SHORT).show();
                        }else{
                            oIsStartTouring = true;
                            oLocations.clear();
                            oId = oDatabaseHelper.getUniqueID();
                            try(SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()){
                                LocalDateTime now = LocalDateTime.now();
                                oDatabaseHelper.putRecordsStartInfo(db, oId, now.format(Const.DATE_FORMAT));
                            }
                            oStartRecordingButton.setText(R.string.stopRecording);
                            Toast.makeText(MapActivity.this, "Touring Start!", Toast.LENGTH_SHORT).show();
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

    private void initializeGps(){
        oFusedClient = LocationServices.getFusedLocationProviderClient(this);
        oSetClient = LocationServices.getSettingsClient(this);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        oLocCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                super.onLocationResult(locationResult);
                Location loc = locationResult.getLastLocation();

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 16f));
                if(oIsStartTouring) {
                    if (isDifferenceEnough(loc)) {
                        try (SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()) {
                            oDatabaseHelper.putPositions(db, oId, oRecordNumber, loc.getLatitude(), loc.getLongitude(), LocalDateTime.now().format(Const.DATE_FORMAT));
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
        };

        oLocReq = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
                .setFastestInterval(500);

        oLocSetReq =  new LocationSettingsRequest.Builder()
                .addLocationRequest(oLocReq)
                .build();
    }

    private void startWatchLocation(){
        oSetClient.checkLocationSettings(oLocSetReq)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if(ActivityCompat.checkSelfPermission(MapActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                            return ;
                        }
                        oFusedClient.requestLocationUpdates(oLocReq, oLocCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("MapMyLocation", e.getMessage());
                    }
                });
    }

    @Override
    protected void onPause(){
        super.onPause();

//        oFusedClient.removeLocationUpdates(oLocCallback);
    }

    @Override
    protected void onResume(){
        super.onResume();
        startWatchLocation();
    }

    private boolean isDifferenceEnough(Location alocation){
        if(oLastMappedLocation == null){
            oLastMappedLocation = alocation;
            return true;
        }

        float distance = alocation.distanceTo(oLastMappedLocation);
        Log.d(LoggerTag.LOCATION, "(latitude, longitude) = (" + alocation.getLatitude() + "," + alocation.getLongitude() + ")");
        Log.d(LoggerTag.LOCATION, "(distance, limit) = (" + distance + "," + Const.DISTANCE_GAP + ")");
        if(distance >= Const.DISTANCE_GAP){
            oLastMappedLocation = alocation;
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void onClick(Map<Integer, LatLng> aMap) {
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
    }
}