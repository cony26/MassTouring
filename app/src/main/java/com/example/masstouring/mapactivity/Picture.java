package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Picture{
    private final Uri oUri;
    private final int oTimeStamp;
    private final  LatLng oLatLng;
    private static final BitmapFactory.Options oBitmapOption = new BitmapFactory.Options();

    static{
        oBitmapOption.inSampleSize = 20;
    }

    Picture(Uri aUri, int aTimeStamp, LatLng aLatLng){
        oUri = aUri;
        oTimeStamp = aTimeStamp;
        oLatLng = aLatLng;
    }

    public Bitmap getBitmap(Context aContext) {
        Bitmap bitmap = null;
        try{
            InputStream stream = aContext.getContentResolver().openInputStream(oUri);
            bitmap = BitmapFactory.decodeStream(new BufferedInputStream(stream), null, oBitmapOption);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }
        return bitmap;
    }

    public int getTimeStamp() {
        return oTimeStamp;
    }

    public LatLng getLatLng() {
        return oLatLng;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("URI:").append(oUri).append(",")
                .append("TimeStamp:").append(oTimeStamp).append(",")
                .append("LatLng:").append(oLatLng.toString());
        return builder.toString();
    }
}
