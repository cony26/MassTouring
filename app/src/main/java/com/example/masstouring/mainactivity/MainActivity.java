package com.example.masstouring.mainactivity;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.OnLifecycleEvent;

import com.example.masstouring.common.LifeCycleLogger;
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
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new LifeCycleLogger(this, getClass().getSimpleName());
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
                        mediaAccessPractice();
                    }
                }
        );
    }

    private void mediaAccessPractice(){

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        List<Image> imageList = new ArrayList<>();

        Uri collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED
        };

        String selection = MediaStore.Images.Media.DATE_ADDED + " <= 10";
        String[] selectionArgs = new String[]{
                String.valueOf(TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES))
        };
        String sortOrder = MediaStore.Images.Media.DISPLAY_NAME + " ASC";

        try(Cursor cursor = getApplicationContext().getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )){
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int countColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

            while(cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                int count = cursor.getInt(countColumn);
                int size = cursor.getInt(sizeColumn);

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                imageList.add(new Image(contentUri, count, size));
            }
        }

        imageList.stream().forEach(e -> Log.d(LoggerTag.SYSTEM_PROCESS, e.toString()));
    }

    class Image{
        private final Uri uri;
        private final int count;
        private final int size;
        public Image(Uri uri, int count, int size){
            this.uri = uri;
            this.count = count;
            this.size = size;
        }

        @Override
        public String toString(){
            StringBuilder builder = new StringBuilder();
            builder.append("uri:").append(uri).append(",")
                    .append("count:").append(count).append(",")
                    .append("size:").append(size);
            return builder.toString();
        }
    }

    private void executeGooglePlaceApi(){
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
