package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Picture implements ClusterItem {
    private final Uri oUri;
    private final int oTimeStamp;
    private final LatLng oLatLng;
    private static int oFusedInt = 0;
    private static final BitmapFactory.Options oBitmapOption = new BitmapFactory.Options();

    static{
        oBitmapOption.inSampleSize = 20;
    }

    Picture(Uri aUri, int aTimeStamp, LatLng aLatLng){
        oUri = aUri;
        oTimeStamp = aTimeStamp;
//        oLatLng = aLatLng;
        oLatLng = new LatLng(aLatLng.latitude, aLatLng.longitude + oFusedInt++*0.1);
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

    @NonNull
    @Override
    public LatLng getPosition() {
        return oLatLng;
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return null;
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
