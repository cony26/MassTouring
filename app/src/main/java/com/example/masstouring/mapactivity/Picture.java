package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.masstouring.common.LoggerTag;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public class Picture implements ClusterItem {
    private final Uri oUri;
    private final int oTimeStamp;
    private final LatLng oLatLng;
    private final int oOrientation;
    private static final BitmapFactory.Options oBitmapOption = new BitmapFactory.Options();
    private static final Matrix oMatrix = new Matrix();

    public Picture(Uri aUri, int aTimeStamp, LatLng aLatLng, int aOrientation){
        oUri = aUri;
        oTimeStamp = aTimeStamp;
        oLatLng = aLatLng;
        oOrientation = aOrientation;
    }

    /**
     * Do not invoke this method from ui thread. This is synchronous method.
     *
     * @param aContext application context
     * @param aReqWidth target width of bitmap
     * @param aReqHeight target height of bitmap
     * @return Bitmap load bitmap from storage based on its URI synchronously.<br>
     *     Bitmap is scaled as much as possible so that the required area is filled with bitmap.
     *     (That is, the bitmap sticks out from the required area.)
     */
    public Bitmap getBitmapSynclyScaledOver(Context aContext, int aReqWidth, int aReqHeight) {
        return loadBitmap(aContext, aReqWidth, aReqHeight, SCALE_OVER_OPERATOR);
    }

    /**
     * Do not invoke this method from ui thread. This is synchronous method.
     *
     * @param aContext application context
     * @param aReqWidth target width of bitmap
     * @param aReqHeight target height of bitmap
     * @return Bitmap load bitmap from storage based on its URI synchronously.<br>
     *     Bitmap is scaled as much as possible so that bitmap is within the required area.
     */
    public Bitmap getBitmapSynclyScaledWithin(Context aContext, int aReqWidth, int aReqHeight) {
        return loadBitmap(aContext, aReqWidth, aReqHeight, SCALE_WITHIN_OPERATOR);
    }

    public Bitmap getItemBitmapAsynclyScaledOver(Context aContext, int aReqWidth, int aReqHeight, OnCompletedLoadBitmapCallback aCallback){
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = loadBitmap(aContext, aReqWidth, aReqHeight, SCALE_OVER_OPERATOR);

                new Handler(Looper.getMainLooper()).post(new Runnable(){
                    @Override
                    public void run() {
                        aCallback.execute(bitmap, Picture.this);
                    }
                });

            }
        });

        Bitmap bitmap = Bitmap.createBitmap(aReqWidth, aReqHeight, Bitmap.Config.ARGB_8888);

        return bitmap;
    }

    public interface OnCompletedLoadBitmapCallback {
        void execute(Bitmap aBitmap, Picture aPicture);
    }

    private static final Function<Bitmap, Function<Integer, Function<Integer, Float>>> SCALE_OVER_OPERATOR =
            bitmap -> aReqWidth -> aReqHeight -> calculateScaleFactorScaledOver(bitmap, aReqWidth, aReqHeight);

    private static final Function<Bitmap, Function<Integer, Function<Integer, Float>>> SCALE_WITHIN_OPERATOR =
            bitmap -> aReqWidth -> aReqHeight -> calculateScaleFactorScaledWithin(bitmap, aReqWidth, aReqHeight);

    private Bitmap loadBitmap(Context aContext, int aReqWidth, int aReqHeight, Function<Bitmap, Function<Integer, Function<Integer, Float>>> aScaleFactorOperator){
        Bitmap bitmap = null;
        try(BufferedInputStream boundsStream = new BufferedInputStream(aContext.getContentResolver().openInputStream(oUri));
            BufferedInputStream actualStream = new BufferedInputStream(aContext.getContentResolver().openInputStream(oUri));
        ){
            if(oOrientation == 90 || oOrientation == 270){
                int tmp = aReqHeight;
                aReqHeight = aReqWidth;
                aReqWidth = tmp;
            }else if(oOrientation == 0 || oOrientation == 180){
                //leave it
            }else{
                Log.e(LoggerTag.MEDIA_ACCESS, "unexpected orientation:" + oOrientation);
            }

            oBitmapOption.inJustDecodeBounds = true;
            oBitmapOption.inSampleSize = 1;
            BitmapFactory.decodeStream(boundsStream, null, oBitmapOption);

            oBitmapOption.inSampleSize = calculateInSampleSize(oBitmapOption, aReqWidth, aReqHeight);
            oBitmapOption.inJustDecodeBounds = false;
            bitmap = BitmapFactory.decodeStream(actualStream, null, oBitmapOption);

            Log.e(LoggerTag.MEDIA_ACCESS, String.format("[JustDecode](w,h)=(%d,%d)", bitmap.getWidth(), bitmap.getHeight()));

            float scaleFactor = aScaleFactorOperator.apply(bitmap).apply(aReqWidth).apply(aReqHeight);
            oMatrix.reset();
            oMatrix.postScale(scaleFactor, scaleFactor);
            oMatrix.postRotate(oOrientation);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), oMatrix, true);
        }catch(IOException | NullPointerException e){
            Log.e(LoggerTag.MEDIA_ACCESS, "bitmap load error {}" , e);
            bitmap = Bitmap.createBitmap(aReqWidth, aReqHeight, Bitmap.Config.ARGB_8888);
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

    private static float calculateScaleFactorScaledOver(Bitmap bitmap, int reqWidth, int reqHeight){
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        float heightRatio = (float)reqHeight / height;
        float widthRatio = (float)reqWidth / width;

        return heightRatio > widthRatio ? heightRatio : widthRatio;
    }

    private static float calculateScaleFactorScaledWithin(Bitmap bitmap, int reqWidth, int reqHeight){
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        float heightRatio = (float)reqHeight / height;
        float widthRatio = (float)reqWidth / width;

        return heightRatio > widthRatio ? widthRatio : heightRatio;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Picture)) return false;
        Picture picture = (Picture) o;
        return oTimeStamp == picture.oTimeStamp &&
                Objects.equals(oUri, picture.oUri) &&
                Objects.equals(oLatLng, picture.oLatLng);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oUri, oTimeStamp, oLatLng);
    }
}
