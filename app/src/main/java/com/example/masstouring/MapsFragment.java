package com.example.masstouring;

import androidx.annotation.NonNull;
import androidx.annotation.PluralsRes;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.TimeUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MapsFragment extends FragmentActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient oFusedClient;
    private SettingsClient oSetClient;
    private LocationSettingsRequest oLocSetReq;
    private LocationCallback oLocCallback;
    private LocationRequest oLocReq;
    private GoogleMap mMap;
    private final DatabaseHelper oDatabaseHelper = new DatabaseHelper(this, DatabaseHelper.DBNAME);
    private int cycleCount = 0;
    private int id;
    private boolean isStartTouring = false;
    private Location oLastMappedLocation = null;
    private static double DISTANCE_GAP = 0.5;
    private final List<Location> oLocations= new ArrayList<>();
    private Button oStartRecordingButton;
    private Button oMemoryButton;
    private boolean oRecordsViewStatus = false;
    private RecyclerView oRecordsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        oStartRecordingButton = findViewById(R.id.btnStartRecording);
        oMemoryButton = findViewById(R.id.btnMemory);
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
                        if(isStartTouring){
                            try(SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()){
                                oDatabaseHelper.putRecordsEndInfo(db, id, Long.toString(SystemClock.currentThreadTimeMillis()), cycleCount);
                            }
                            isStartTouring = false;
                            cycleCount = 0;
                            Toast.makeText(MapsFragment.this, "Touring Finish!", Toast.LENGTH_SHORT).show();
                        }else{
                            isStartTouring = true;
                            oLocations.clear();
                            oDatabaseHelper.debugPrint();
                            id = oDatabaseHelper.getUniqueID();
                            Log.d("ID", Integer.toString(id));
                            try(SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()){
                                oDatabaseHelper.putRecordsStartInfo(db, id, Long.toString(SystemClock.currentThreadTimeMillis()));
                            }
                            Toast.makeText(MapsFragment.this, "Touring Start!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        oMemoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(oRecordsViewStatus) {
                    oRecordsView.setEnabled(false);
                }else{
                    oRecordsView = (RecyclerView) findViewById(R.id.recordsView);
                    LinearLayoutManager manager = new LinearLayoutManager(MapsFragment.this);
                    manager.setOrientation(LinearLayoutManager.VERTICAL);
                    oRecordsView.setLayoutManager(manager);
                    List<RecordsItem> data = loadRecords();
                    RecyclerView.Adapter adapter = new RecordsViewAdapter(data);
                    oRecordsView.setAdapter(adapter);
                    oRecordsView.setEnabled(false);
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
            Log.d("RecordsItem", item.toString());
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
                if(isStartTouring) {
                    if (isDifferenceEnough(loc)) {
                        try (SQLiteDatabase db = oDatabaseHelper.getWritableDatabase()) {
                            oDatabaseHelper.putPositions(db, id, cycleCount, loc.getLatitude(), loc.getAltitude());
                        }
                        oLocations.add(oLastMappedLocation);
                        cycleCount++;
                    }
                    oDatabaseHelper.debugPrint();
                }

                if(cycleCount > 0){
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
                        if(ActivityCompat.checkSelfPermission(MapsFragment.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
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
        oFusedClient.removeLocationUpdates(oLocCallback);
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
        Log.d("Distance", "(distance, limit) = (" + distance + "," + DISTANCE_GAP + ")");
        if(distance >= DISTANCE_GAP){
            oLastMappedLocation = alocation;
            return true;
        }else{
            return false;
        }
    }
}