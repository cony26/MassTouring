package com.example.masstouring.mainactivity;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.OnLifecycleEvent;

import com.example.masstouring.common.LifeCycleLogger;
import com.example.masstouring.common.LoggerTask;
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

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private int count = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoggerTask.initialize(getApplicationContext());
        new LifeCycleLogger(this, getClass().getSimpleName());
        setContentView(R.layout.activity_main);
        setButtonClickListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoggerTask.getInstance().setMainActivityState(false);
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

        final DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        Date date = null;
        try {
            date = df.parse("2020/11/23");
        }catch(ParseException e){
            e.printStackTrace();
        }
        String selection = MediaStore.Images.Media.DATE_ADDED + " >= " + (int)(date.getTime() / 1000);
        String[] selectionArgs = new String[]{

        };
        String sortOrder = MediaStore.Images.Media._ID + " ASC";

        try(Cursor cursor = getApplicationContext().getContentResolver().query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
        )){
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

            while(cursor.moveToNext()){
                long id = cursor.getLong(idColumn);
                int count = cursor.getInt(dateAddedColumn);
                int size = cursor.getInt(sizeColumn);

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                imageList.add(new Image(contentUri, count, size));
            }
        }

        imageList.stream().forEach(e -> Log.d(LoggerTag.SYSTEM_PROCESS, e.toString()));
        try{
            InputStream stream = getApplicationContext().getContentResolver().openInputStream(imageList.get(count++).uri);
            Bitmap bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream));
            ImageView view = new ImageView(this);
            view.setImageBitmap(bitmap);
            this.addContentView(view, new LinearLayout.LayoutParams(100,100 ));
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

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
                    .append("count:").append(new Date(count * 1000L).toString()).append(",")
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
