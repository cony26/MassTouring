package com.example.masstouring.mapactivity;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class DistributedItem {
    private int oCenterX;
    private int oCenterY;
    private int oRadius;
    private double oTheta;
    private boolean oCW;
    private Bitmap oBitmap;
    private Rect oRect;
    DistributedItem(int aCenterX, int aCenterY, int aRadius, double aTheta, boolean aCW, Bitmap aBitmap, Rect aRect){
        oCenterX = aCenterX;
        oCenterY = aCenterY;
        oRadius = aRadius;
        oTheta = aTheta;
        oCW = aCW;
        oBitmap = aBitmap;
        oRect = aRect;
    }

    public int getCenterX() {
        return oCenterX;
    }

    public void setCenterX(int aCenterX) {
        oCenterX = aCenterX;
    }

    public int getCenterY() {
        return oCenterY;
    }

    public void setCenterY(int aCenterY) {
        oCenterY = aCenterY;
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

    public void updateRect(int aDistance){
        double theta = (double) aDistance / oRadius;
        oTheta += oCW ? theta : -theta;

        int centerX = (int) (oRadius * Math.cos(oTheta)) + oCenterX;
        int centerY = (int) (oRadius * Math.sin(oTheta)) + oCenterY;
        int w = oBitmap.getWidth();
        int h = oBitmap.getHeight();
        oRect.offsetTo(centerX - w / 2, centerY - h / 2);
    }
}
