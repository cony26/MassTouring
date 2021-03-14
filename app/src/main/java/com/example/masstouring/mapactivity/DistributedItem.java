package com.example.masstouring.mapactivity;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import androidx.annotation.NonNull;

public class DistributedItem {
    private int oCenterX;
    private int oCenterY;
    private int oRadius;
    private double oTheta;
    private final boolean oCW;
    private final Bitmap oBitmap;
    private final Rect oRect;
    private final Picture oPicture;
    DistributedItem(int aCenterX, int aCenterY, int aRadius, double aTheta, boolean aCW, Bitmap aBitmap, Rect aRect, Picture aPicture){
        oCenterX = aCenterX;
        oCenterY = aCenterY;
        oRadius = aRadius;
        oTheta = aTheta;
        oCW = aCW;
        oBitmap = aBitmap;
        oRect = aRect;
        oPicture = aPicture;
    }

    public int getCenterX(){
        return oCenterX;
    }

    public int getCenterY(){
        return oCenterY;
    }

    public int getRadius() {
        return oRadius;
    }

    public void setRadius(int aRadius) {
        oRadius = aRadius;
    }

    public Bitmap getBitmap() {
        return oBitmap;
    }

    public Rect getRect() {
        return oRect;
    }

    public Picture getPicture(){
        return oPicture;
    }

    public void updateRect(int aDistance){
        double theta = (double) aDistance / oRadius;
        oTheta += oCW ? theta : -theta;

        int centerX = (int) (oRadius * Math.cos(oTheta)) + oCenterX;
        int centerY = (int) (oRadius * Math.sin(oTheta)) + oCenterY;
        int w = oBitmap.getWidth();
        int h = oBitmap.getHeight();
        oRect.offsetTo(centerX - w / 2, centerY - h / 2);
    }

    public void updateCenterPoint(Point aPoint){
        oCenterX = aPoint.x;
        oCenterY = aPoint.y;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("centerX:").append(oCenterX).append(",")
                .append("centerY:").append(oCenterY).append(",")
                .append("radius:").append(oRadius).append(",")
                .append("theta:").append(oTheta).append(",")
                .append("CW:").append(oCW).append(",")
                .append("Rect:").append(oRect);
        return builder.toString();
    }
}
