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
import java.io.IOException;

public class Picture implements ClusterItem {
    private final Uri oUri;
    private final int oTimeStamp;
    private final LatLng oLatLng;
    private static int oFusedInt = 0;
    private static final BitmapFactory.Options oBitmapOption = new BitmapFactory.Options();

    Picture(Uri aUri, int aTimeStamp, LatLng aLatLng){
        oUri = aUri;
        oTimeStamp = aTimeStamp;
//        oLatLng = aLatLng;
        oLatLng = new LatLng(aLatLng.latitude, aLatLng.longitude + oFusedInt++*0.1);
    }

    /**
     *
     * @param aContext application context
     * @param oReqWidth target width of bitmap
     * @param oReqHeight target height of bitmap
     * @return Bitmap load bitmap from storage based on its URI.<br>
     *     Bitmap is scaled to {@code oReqWidth} or {@code oReqHeight} so that bitmap doesn't have padding.
     */
    public Bitmap getBitmap(Context aContext, int oReqWidth, int oReqHeight) {
        Bitmap bitmap = null;
        try(BufferedInputStream boudStream = new BufferedInputStream(aContext.getContentResolver().openInputStream(oUri));
            BufferedInputStream actualStream = new BufferedInputStream(aContext.getContentResolver().openInputStream(oUri));
        ){
            oBitmapOption.inJustDecodeBounds = true;
            oBitmapOption.inSampleSize = 1;
            bitmap = BitmapFactory.decodeStream(boudStream, null, oBitmapOption);

            oBitmapOption.inSampleSize = calculateInSampleSize(oBitmapOption, oReqWidth, oReqHeight);
            oBitmapOption.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeStream(actualStream, null, oBitmapOption);

            double scaleFactor = calculateScaleFactor(bitmap, oReqWidth, oReqHeight);
            bitmap = Bitmap.createScaledBitmap(bitmap, (int)(bitmap.getWidth() * scaleFactor), (int)(bitmap.getHeight() * scaleFactor), true);
        }catch(IOException e){
            e.printStackTrace();
        }
        return bitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options aOptions, int reqWidth, int reqHeight){
        final int height = aOptions.outHeight;
        final int width = aOptions.outWidth;
        int inSampleSize = 1;

        if(height > reqHeight || width > reqWidth){
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth){
                inSampleSize *= 2;
            }
        }

        //for the maximum scaling, multiply 2 more one time.
        return inSampleSize * 2;
    }

    /** scale bitmap to match the bigger one so that bitmap doesn't have the padding.
     */
    private static double calculateScaleFactor(Bitmap bitmap, int reqWidth, int reqHeight){
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        double heightRatio = (double)reqHeight / height;
        double widthRatio = (double)reqWidth / width;

        return heightRatio > widthRatio ? heightRatio : widthRatio;
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
