package com.example.masstouring.mapactivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

import java.util.List;

public class FocusedDrawable {
    private List<DistributedItem> oDistributedItems;
    private int oIndex;
    private DistributedItem oFocusedItem = null;
    private Rect oFocusedWindowRect;
    private Bitmap oBitmap;
    private Bitmap oNextBitmap;
    private Bitmap oPrevBitmap;
    private int oMaxWidth;
    private int oMaxHeight;
    private boolean oEnabled = false;
    private final Context oContext;

    FocusedDrawable(Context aContext){
        oContext = aContext;
    }

    public DistributedItem getDistributedItem() {
        return oFocusedItem;
    }

    public void prepareToShow(DistributedItem aOriginalItem, List<DistributedItem> aDistributedItems, View aView) {
        oFocusedItem = aOriginalItem;
        oDistributedItems = aDistributedItems;
        oIndex = oDistributedItems.indexOf(oFocusedItem);

        Point point = new Point();
        aView.getDisplay().getRealSize(point);
        oMaxWidth = point.x;
        oMaxHeight = point.y;

        oBitmap = oFocusedItem.getPicture().getBitmapSynclyScaledWithin(oContext, oMaxWidth, oMaxHeight);
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                int nextIndex = oIndex + 1;
                if(nextIndex == oDistributedItems.size()){
                    nextIndex = 0;
                }

                int prevIndex = oIndex - 1;
                if(prevIndex == -1){
                    prevIndex = oDistributedItems.size() - 1;
                }

                oNextBitmap = oDistributedItems.get(nextIndex).getPicture().getBitmapSynclyScaledWithin(oContext, oMaxWidth, oMaxHeight);
                oPrevBitmap = oDistributedItems.get(prevIndex).getPicture().getBitmapSynclyScaledWithin(oContext, oMaxWidth, oMaxHeight);
            }
        });

        int centerX = oMaxWidth / 2;
        int centerY = oMaxHeight / 2;
        int halfWidth = oBitmap.getWidth() / 2;
        int halfHeight = oBitmap.getHeight() / 2;
        oFocusedWindowRect = new Rect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
        oEnabled = true;
    }

    public boolean isEnabled(){
        return oEnabled;
    }

    public void setEnable(boolean aEnable){
        oEnabled = aEnable;
    }

    public void next(){
        oPrevBitmap = oBitmap;
        oBitmap = oNextBitmap;

        int nextIndex = oIndex + 1;
        if(nextIndex == oDistributedItems.size()){
            nextIndex = 0;
        }
        oIndex = nextIndex;
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                oNextBitmap = oDistributedItems.get(oIndex).getPicture().getBitmapSynclyScaledWithin(oContext, oMaxWidth, oMaxHeight);
            }
        });

        int centerX = oMaxWidth / 2;
        int centerY = oMaxHeight / 2;
        int halfWidth = oBitmap.getWidth() / 2;
        int halfHeight = oBitmap.getHeight() / 2;
        oFocusedWindowRect = new Rect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
    }

    public void previous(){
        oNextBitmap = oBitmap;
        oBitmap = oPrevBitmap;

        int prevIndex = oIndex - 1;
        if(prevIndex == -1){
            prevIndex = oDistributedItems.size() - 1;
        }
        oIndex = prevIndex;
        MapActivity.cExecutors.execute(new Runnable() {
            @Override
            public void run() {
                oPrevBitmap = oDistributedItems.get(oIndex).getPicture().getBitmapSynclyScaledWithin(oContext, oMaxWidth, oMaxHeight);
            }
        });

        int centerX = oMaxWidth / 2;
        int centerY = oMaxHeight / 2;
        int halfWidth = oBitmap.getWidth() / 2;
        int halfHeight = oBitmap.getHeight() / 2;
        oFocusedWindowRect = new Rect(centerX - halfWidth, centerY - halfHeight, centerX + halfWidth, centerY + halfHeight);
    }

    public void draw(Canvas aCanvas, Paint p){
        if(oEnabled){
            aCanvas.drawColor(Color.BLACK);
            aCanvas.drawBitmap(oBitmap, null, oFocusedWindowRect, p);
        }
    }
}
