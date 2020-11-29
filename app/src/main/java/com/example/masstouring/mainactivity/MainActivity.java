package com.example.masstouring.mainactivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.masstouring.mapactivity.MapActivity;
import com.example.masstouring.R;
import com.example.masstouring.common.LoggerTag;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LoggerTag.SYSTEM_PROCESS, "onCreate MainActivity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setButtonClickListeners();
    }

    private void setButtonClickListeners() {
        findViewById(R.id.btnOpenMap).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent i = new Intent(MainActivity.this, MapActivity.class);
                        startActivity(i);
                    }
                }
        );

        findViewById(R.id.btnCheckPictures).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Places.initialize(getApplicationContext(), getText(R.string.google_maps_key).toString());
                        PlacesClient placesClient = Places.createClient(MainActivity.this);
                        List<Place.Field> fields = new ArrayList<>();
                        fields.add(Place.Field.ADDRESS);
                        fields.add(Place.Field.NAME);
                        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.newInstance(fields);
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        Task<FindCurrentPlaceResponse> placeResponse = placesClient.findCurrentPlace(request);
                        placeResponse.addOnCompleteListener(task -> {
                            if(task.isSuccessful()){
                                FindCurrentPlaceResponse response = task.getResult();
                                for(PlaceLikelihood placeLikelihood : response.getPlaceLikelihoods()){
                                    Log.d("test", String.format("Place '%s' has likelihood: %f", placeLikelihood.getPlace().getName(), placeLikelihood.getLikelihood()));

                                }
                            }else{
                                Exception exception = task.getException();
                                if(exception instanceof ApiException){
                                    ApiException apiException = (ApiException) exception;
                                    Log.e("test", "Place not found:" + apiException.getStatusCode());
                                }
                            }
                        });

                    }
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onDestroy MainActivity");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onPause MainActivity");
    }

    @Override
    protected void onResume(){
        super.onResume();
        Log.d(LoggerTag.SYSTEM_PROCESS,"onResume MainActivity");
    }
}
