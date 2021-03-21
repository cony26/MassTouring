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
    private boolean oCW;
    private final Bitmap oBitmap;
    private final Rect oRect;
    private final Picture oPicture;

    DistributedItem(Bitmap aBitmap, Picture aPicture){
        oBitmap = aBitmap;
        oRect = new Rect(0, 0, aBitmap.getWidth(), aBitmap.getHeight());
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

    public Bitmap getBitmap() {
        return oBitmap;
    }

    public Rect getRect() {
        return oRect;
    }

    public Picture getPicture(){
        return oPicture;
    }

    /**
     *
     * @param aDistance the distance (px) which this item moves on along with the circle.
     */
    public void updatePositionByDistancePx(int aDistance){
        updatePositionByTheta((double) aDistance / oRadius);
    }

    private void updatePositionByTheta(double aTheta){
        oTheta += oCW ? aTheta : -aTheta;
        updateRect();
    }

    private void updateRect(){
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

    public void update(int aCenterX, int aCenterY, int aRadius, double aTheta, boolean aCW){
        oCenterX = aCenterX;
        oCenterY = aCenterY;
        oRadius = aRadius;
        oTheta = aTheta;
        oCW = aCW;
        updateRect();
    }

    private Rect createRectOnCircle(){
        int centerX = (int) (oRadius * Math.cos(oTheta)) + oCenterX;
        int centerY = (int) (oRadius * Math.sin(oTheta)) + oCenterY;
        int w = oBitmap.getWidth();
        int h = oBitmap.getHeight();
        return new Rect(centerX - w / 2, centerY - h / 2, centerX + w / 2, centerY + h /2);
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
